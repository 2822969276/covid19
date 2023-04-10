package cn.itcast.process

import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.streaming.{Seconds, StreamingContext}

import java.lang

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
      ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG -> (true: lang.Boolean), //是否自动提交偏移量
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer],
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer]
    )
    val topics: Array[String] = Array("covid19_wz")
    //3.连接kafka获取消息
    val kafkaDS: InputDStream[ConsumerRecord[String, String]] = KafkaUtils.createDirectStream[String,String](
      ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](topics, kafkaParams)
    )
    //4.实时处理数据
    val valueDS: DStream[String] = kafkaDS.map(_.value())
    valueDS.print()
    //5.将处理分析的结果存入到Mysql

    //6.开启SparkStreaming任务并等待结束
    ssc.start()
    ssc.awaitTermination()
  }
}
