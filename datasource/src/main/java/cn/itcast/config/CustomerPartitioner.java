package cn.itcast.config;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import java.util.Map;

/**
 * 自定义分区器指定分区规则（默认市按照key的hash）
 */
public class CustomerPartitioner implements Partitioner {
    //根据参数按照指定的规则进行分区，返回分区编号即可
    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes1, Cluster cluster) {
        Integer k = (Integer) key;
        Integer num = cluster.partitionCountForTopic(topic);//主题的分区数
        int partition = k % num;
        return partition;
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> map) {

    }
}
