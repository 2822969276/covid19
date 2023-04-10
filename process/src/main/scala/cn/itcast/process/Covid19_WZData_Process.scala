package cn.itcast.process

import cn.itcast.util.OffsetUtils
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{CanCommitOffsets, ConsumerStrategies, HasOffsetRanges, KafkaUtils, LocationStrategies, OffsetRange}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.streaming.{Seconds, StreamingContext}

import java.lang
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
    val offsetsMap :mutable.Map[TopicPartition,Long] = OffsetUtils.getOffsetsMap("SparkKafka","covid19_wz")

    val kafkaDS: InputDStream[ConsumerRecord[String, String]] = if(offsetsMap.size>0){
      println("Mysql记录了offset信息，从offset处开始消费")
      //3.连接kafka获取消息
      KafkaUtils.createDirectStream[String, String](
        ssc,
        LocationStrategies.PreferConsistent,
        ConsumerStrategies.Subscribe[String, String](topics, kafkaParams,offsetsMap)
      )
    }else{
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

    //5.将处理分析的结果存入到Mysql

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
              for (o <- offsets) {
                print(s"topic=${o.topic},partition=${o.partition},fromOffset=${o.fromOffset},until=${o.untilOffset}")
              }
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
