package cn.itcast.process

import cn.itcast.util.OffsetUtils
import com.alibaba.fastjson.{JSON, JSONObject}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{CanCommitOffsets, ConsumerStrategies, HasOffsetRanges, KafkaUtils, LocationStrategies, OffsetRange}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.streaming.{Seconds, StreamingContext}

import java.lang
import java.sql.{Connection, DriverManager, PreparedStatement}
import scala.collection.mutable

/**
 * 疫情物资数据的实时处理与分析
 */
object Covid19_WZData_Process {
    def main(args: Array[String]): Unit = {
        //1.准备sparkStreaming的开发环境
        val sparkConf = new SparkConf().setAppName("Covid19_WZData_Process").setMaster("local[*]")
        val sc: SparkContext = new SparkContext(sparkConf)
        sc.setLogLevel("WARN")
        val ssc: StreamingContext = new StreamingContext(sc, Seconds(5))
        ssc.checkpoint("./sscckp")

        //    补充知识点：SparkStreaming整合Kafka的两种方式：
        //    1.Reciver模式
        //    KafkaUtils.createDStream--API创建
        //    会有一个Receiver作为常驻Task运行在Executor进程中，一直等待数据的到来
        //    一个Receiver效率会比较低，那么可以使用多个Receiver，但是多个Receiver中的数据又需要手动进行Union（合并）很麻烦
        //    且其中某个Receiver挂了，会导致数据丢失，需要开启WAL预写日志来保证数据安全，但是效率又低了
        //    Receiver模式使用Zookeeper来连接Kafka（Kafka的新版本中已经不推荐使用该方式了）
        //    Receiver模式使用的时Kafka的高阶API（高度封装的），offset由Receiver提交到ZK中（Kafka的新版本中offset默认存储在默认主题__consumer__offset中的，不推荐存入ZK中了），
        //    容易和Spark维护在Checkpoint中的offset不一致
        //    所以不管从何种角度去说Receiver模式都已经不再适合现如今的Kafka版本了，面试的时候要说出以上的原因！！
        //
        //    2.Direct模式
        //    KafkaUtils.createDirectStream--API创建
        //    Direct模式是直接连接到Kafka的各个分区，并拉取数据，提高了数据读取的并发能力
        //    Direct模式使用的是Kafka低阶API（底层API），可以自己维护偏移量到任何地方
        //    （默认是由Spark提交到默认主题/Checkpoint）
        //    Direct模式+手动操作可以保证数据的Exactly-Once精准一次（数据仅会被处理一次）

        //    补充知识点：SparkStreaming整合Kafka的两个版本的API
        //    Spark-streaming-kafka-0-8
        //    支持Receiver模式和Direct模式，但是不支持offset维护API，不支持动态分区订阅。。
        //
        //    Spark-streaming-kafka-0-10
        //    支持Direct，不支持Receiver模式，支持offset维护API，支持动态分区订阅。。
        //    结论：使用Spark-Streaming-kafka-0-10版本即可


        //2.准备kafka的连接参数
        val kafkaParams: Map[String, Object] = Map[String, Object](
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> "node01:9092,node02:9092,node03:9092",
            ConsumerConfig.GROUP_ID_CONFIG -> "SparkKafka",
            //latest表示如果记录了偏移量则从记录的位置开始消费，如果没有记录则从最新/最后的位置开始消费
            //earliest表示如果记录了偏移量则从记录的位置开始消费，如果没有记录则从最开始/最早的位置开始消费
            //none表示如果记录了偏移量则从记录的位置开始消费，如果没有记录则报错
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "latest", //偏移量重置位置
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG -> (false: lang.Boolean), //是否自动提交偏移量
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer],
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer]
        )
        val topics: Array[String] = Array("covid19_wz")

        //从MySQL中查询出offset:Map[TopicPartition,Long]信息
        val offsetsMap: mutable.Map[TopicPartition, Long] = OffsetUtils.getOffsetsMap("SparkKafka", "covid19_wz")

        val kafkaDS: InputDStream[ConsumerRecord[String, String]] = if (offsetsMap.size > 0) {
            println("Mysql记录了offset信息，从offset处开始消费")
            //3.连接kafka获取消息
            KafkaUtils.createDirectStream[String, String](
                ssc,
                LocationStrategies.PreferConsistent,
                ConsumerStrategies.Subscribe[String, String](topics, kafkaParams, offsetsMap)
            )
        } else {
            println("Mysql没有记录offset信息，从latest处开始消费")
            //3.连接kafka获取消息
            KafkaUtils.createDirectStream[String, String](
                ssc,
                LocationStrategies.PreferConsistent,
                ConsumerStrategies.Subscribe[String, String](topics, kafkaParams)
            )
        }


        //4.实时处理数据并手动提交偏移量
        //    val valueDS: DStream[String] = kafkaDS.map(_.value())
        //    valueDS.print()
        //需求分析
        //    {"count":673,"from":"消耗","name":"N95口罩/个"}
        //我们从Kafka中消费的数据为如上格式的jsonStr，需要解析为json对象（或者是样例类）
        //目标是将数据转换为如下格式：
        //  名称，   "采购","下拨","捐赠","消耗","需求"，库存
        //N95口罩/个，1000，1000，  500，-1000，-1000，500
        //为了达成目标格式，需要对每一条数据进行处理，得出如下格式：
        //(name,("采购","下拨","捐赠","消耗","需求"，库存))
        //如：接收到一条数据为：
        //    {"count":673,"from":"消耗","name":"N95口罩/个"}
        //应该记为：
        //(N95口罩/个，(采购0，下拨0,捐赠0，消耗-673，需求0，库存-673))
        //再来一条数据：
        //{"count":500,"from":"采购","name":"N95口罩/个"}
        //应该记为：
        //(N95口罩/个，(采购500，下拨0,捐赠0，消耗0，需求0，库存500))
        //最后聚合结果
        //    (N95口罩/个，(采购500，下拨0,捐赠0，消耗-673，需求0，库存-173))
        //4.1将接收到的数据转换为需要的元组格式：
        val tupleDS: DStream[(String, (Int, Int, Int, Int, Int, Int))] = kafkaDS.map(
            record => {
                val jsonStr: String = record.value()
                val jsonObject: JSONObject = JSON.parseObject(jsonStr)
                val name: String = jsonObject.getString("name")
                val from: String = jsonObject.getString("from")
                val count: Int = jsonObject.getInteger("count")
                //根据物资来源不同，将count记在不同的位置，最终形成统一的格式
                from match {
                    case "采购" => (name, (count, 0, 0, 0, 0, count))
                    case "下拨" => (name, (0, count, 0, 0, 0, count))
                    case "捐赠" => (name, (0, 0, count, 0, 0, count))
                    case "消耗" => (name, (0, 0, 0, -count, 0, -count))
                    case "需求" => (name, (0, 0, 0, 0, -count, -count))
                }
            }
        )
        //4.2将上述格式的数据按照key进行聚合（有状态的计算）--使用updateStateByKey
        //定义一个函数，用来将当前批次的数据和历史数据进行聚合
        val updateFunc = (currentValues: Seq[(Int, Int, Int, Int, Int, Int)], historyValue: Option[(Int, Int, Int, Int, Int, Int)]) => {
            //0.定义变量，接收当前批次数据
            var current_cg: Int = 0
            var current_xb: Int = 0
            var current_jz: Int = 0
            var current_xh: Int = 0
            var current_xq: Int = 0
            var current_kc: Int = 0
            if (currentValues.size > 0) {
                //1.取出当前批次数据
                for (currentValue <- currentValues) {
                    current_cg += currentValue._1
                    current_xb += currentValue._2
                    current_jz += currentValue._3
                    current_xh += currentValue._4
                    current_xq += currentValue._5
                    current_kc += currentValue._6
                }
                //2.取出历史数据
                val history_cg: Int = historyValue.getOrElse((0, 0, 0, 0, 0, 0))._1
                val history_xb: Int = historyValue.getOrElse((0, 0, 0, 0, 0, 0))._2
                val history_jz: Int = historyValue.getOrElse((0, 0, 0, 0, 0, 0))._3
                val history_xh: Int = historyValue.getOrElse((0, 0, 0, 0, 0, 0))._4
                val history_xq: Int = historyValue.getOrElse((0, 0, 0, 0, 0, 0))._5
                val history_kc: Int = historyValue.getOrElse((0, 0, 0, 0, 0, 0))._6
                //3.将当前批次数据和历史数据聚合
                val result_cg: Int = current_cg + history_cg
                val result_xb: Int = current_xb + history_xb
                val result_jz: Int = current_jz + history_jz
                val result_xh: Int = current_xh + history_xh
                val result_xq: Int = current_xq + history_xq
                val result_kc: Int = current_kc + history_kc
                //4.将聚合结果返回
                Some((result_cg, result_xb, result_jz, result_xh, result_xq, result_kc))
            } else {
                historyValue
            }
        }
        val resultDS: DStream[(String, (Int, Int, Int, Int, Int, Int))] = tupleDS.updateStateByKey(updateFunc)

        //5.将处理分析的结果存入到Mysql
        /*
        CREATE TABLE `covid19_wz`(
            `name` varchar(12) NOT NULL DEFAULT '',
            `cg` int(11) DEFAULT '0',
            `xb` int(11) DEFAULT '0',
            `jz` int(11) DEFAULT '0',
            `xh` int(11) DEFAULT '0',
            `xq` int(11) DEFAULT '0',
            `kc` int(11) DEFAULT '0',
            PRIMARY KEY (`name`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
         */
        resultDS.foreachRDD(
            rdd => {
                rdd.foreachPartition(
                    lines => {
                        //1.开启连接
                        val conn: Connection = DriverManager.getConnection("jdbc:mysql://192.168.80.80:3306/bigdata", "root", "000000")
                        //2.编写sql并获取ps
                        val sql:String = "replace into covid19_wz(name,cg,xb,jz,xh,xq,kc) values(?,?,?,?,?,?,?);"
                        val ps: PreparedStatement = conn.prepareStatement(sql)
                        //3.设置参数并执行
                        for (line <- lines){
                            ps.setString(1,line._1)
                            ps.setInt(2,line._2._1)
                            ps.setInt(3,line._2._2)
                            ps.setInt(4,line._2._3)
                            ps.setInt(5,line._2._4)
                            ps.setInt(6,line._2._5)
                            ps.setInt(7,line._2._6)
                            ps.executeUpdate()
                        }
                        //4.关闭资源
                        ps.close()
                        conn.close()
                    }
                )
            }
        )
        //6.手动提交偏移量
        //我们要手动提交偏移量，那么意味着，消费了一批数据就应该提交一次偏移量
        //在SparkStreaming中数据抽象为DStream，DStream的底层其实也就是RDD，也就是每一批次的数据
        //所以接下来我们应该对DStream中的RDD进行处理
        kafkaDS.foreachRDD(
            rdd => {
                if (rdd.count() > 0) { //如果该RDD中有数据，则处理
                    rdd.foreach(
                        record => {
                            print("从Kafka中消费到的每一条消息：" + record)
                            //获取偏移量
                            //使用Spark-streaming-kafka-0-10封装好的API来存放偏移量并提交
                            val offsets: Array[OffsetRange] = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
                            //              for (o <- offsets) {
                            //                print(s"topic=${o.topic},partition=${o.partition},fromOffset=${o.fromOffset},until=${o.untilOffset}")
                            //              }
                            //手动提交偏移量到Kafka的默认主题：__consumer__offsets中，如果开启了Checkpoint还会提交到checkpoint中
                            //              kafkaDS.asInstanceOf[CanCommitOffsets].commitAsync(offsets)
                            OffsetUtils.saveOffsets("SparkKafka", offsets)
                        }
                    )
                }
            }
        )
        //7.开启SparkStreaming任务并等待结束
        ssc.start()
        ssc.awaitTermination()
    }
}
