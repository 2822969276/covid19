package cn.itcast.process

import cn.itcast.bean.{CovidBean, StatisticsDataBean}
import com.alibaba.fastjson.JSON
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql._
import org.apache.spark.sql.streaming.Trigger

import java.util
import scala.collection.mutable

object Covid19_Data_Process {
    def main(args: Array[String]): Unit = {
        //1.创建StructuredStreaming执行环境
        //StructuredStreaming支持使用SQL来处理实时流数据，数据抽象和SparkSQL一样，也是DataFrame和DataSet
        //所以这里创建StructuredStreaming执行环境就直接创建SparkSession即可
        val spark: SparkSession = SparkSession.builder().master("local[*]").appName("Covid19_Data_Process").getOrCreate()
        val sc: SparkContext = spark.sparkContext
        sc.setLogLevel("WARN")
        //导入隐式转换方便后续使用
        import spark.implicits._ //DF、DS和RDD相互转换的
        import org.apache.spark.sql.functions._ //导入spark SQL的一些内置函数
        import scala.collection.JavaConversions._//将java的集合隐式转换成Scala的集合

        //2.连接Kafka
        //从Kafka接收消息
        val kafkaDF: DataFrame = spark.readStream
          .format("kafka")
          .option("kafka.bootstrap.servers", "node01:9092,node02:9092,node03:9092")
          .option("subscribe", "covid19")
          .load()
        //取出消息中的value
        val jsonStrDS: Dataset[String] = kafkaDF.selectExpr("CAST(value AS STRING)").as[String]
//        jsonStrDS.writeStream
//          .format("console")//设置输出目的地
//          .outputMode("append")//输出模式，默认就是append表示显示新增行
//          .trigger(Trigger.ProcessingTime(0))//触发间隔，0表示尽可能快的执行
//          .option("truncate",false)
//          .start()
//          .awaitTermination()


        //3.处理数据
        //将jsonStr转为样例类
        val covidBeanDS: Dataset[CovidBean] = jsonStrDS.map(
            jsonStr => {
                JSON.parseObject(jsonStr, classOf[CovidBean]) //这里java中使用类名.class
            }
        )
        //分离出省份数据
        val provinceDS: Dataset[CovidBean] = covidBeanDS.filter(_.statisticsData != null)
        //分离出城市数据
        val cityDS: Dataset[CovidBean] = covidBeanDS.filter(_.statisticsData == null)
        //分离出各省份每一天的统计数据
        val statisticsDataDS: Dataset[StatisticsDataBean] = provinceDS.flatMap(
            p => {
                val jsonStr: String = p.statisticsData
                val list: mutable.Buffer[StatisticsDataBean] = JSON.parseArray(jsonStr, classOf[StatisticsDataBean])
                val newList: mutable.Buffer[StatisticsDataBean] = list.map(
                    s => {
                        s.provinceShortName = p.provinceShortName
                        s.locationId = p.locationId
                        s
                    }
                )
                newList
            }
        )
        //4.统计分析

        //5.结果输出
    }
}
