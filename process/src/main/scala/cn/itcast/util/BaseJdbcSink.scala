package cn.itcast.util

import com.mysql.jdbc.Connection
import org.apache.spark.sql.{ForeachWriter, Row}

import java.sql.{DriverManager, PreparedStatement}

abstract class BaseJdbcSink(sql:String) extends ForeachWriter[Row]{
    def realProcess(str: String, row: Row)

    var conn:Connection = _
    var ps :PreparedStatement = _
    //开启连接
    override def open(partitionId: Long, version: Long): Boolean = {
        conn = DriverManager.getConnection("jdbc:mysql://192.168.80.80/bigdata","root","000000")
        true
    }

    //处理数据--将数据存入到MySQL
    override def process(value: Row): Unit = {
        realProcess(sql,value)
    }

    //关闭连接
    override def close(errorOrNull: Throwable): Unit = {
        if (conn!=null) {
            conn.close()
        }
        if (ps!=null) {
            ps.close()
        }
    }
}
