package com.twitter_trending_topics;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.List;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * Keeps stats on word count, calculates and logs top words every X second to stdout and top list every Y seconds,
 *
 * @author davidk
 */
public class WordCounterBolt extends BaseRichBolt {

    private static final long serialVersionUID = 2706047697068872387L;

    private static final Logger logger = LoggerFactory.getLogger(WordCounterBolt.class);

    /**
     * Number of seconds before the top list will be logged to stdout.
     */
    private final long logIntervalSec;

    /**
     * Number of seconds before the top list will be cleared.
     */
    private final long clearIntervalSec;

    /**
     * Number of top words to store in stats.
     */
    private final int topListSize;

    private Map<String, Long> counter;
    private long lastLogTime;
    private long lastClearTime;

    public WordCounterBolt(long logIntervalSec, long clearIntervalSec, int topListSize) {
        //logger.info("WordCounterBolt INSTANCE CREATED \n ");
        this.logIntervalSec = logIntervalSec;
        this.clearIntervalSec = clearIntervalSec;
        this.topListSize = topListSize;
    }

    public void prepare(Map map, TopologyContext topologyContext, OutputCollector collector) {
        //logger.info("WordCounterBolt PREPARE METHOD WORKED \n ");
        counter = new HashMap<String, Long>();
        lastLogTime = System.currentTimeMillis();
        lastClearTime = System.currentTimeMillis();
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        //logger.info("WordCounterBolt DECLARE METHOD WORKED \n ");
    }

    @Override
    public void execute(Tuple input) {
        //logger.info("WordCounterBolt EXECUTE METHOD WORKED \n ");
        //logger.info("WordCounterBolt tuple input : {} \n ", input);
        String word = (String) input.getValueByField("word");
        Long count = counter.get(word);
        count = count == null ? 1L : count + 1;
        counter.put(word, count);

//        logger.info(new StringBuilder(word).append('>').append(count).toString());

        long now = System.currentTimeMillis();
        long logPeriodSec = (now - lastLogTime) / 1000;
        if (logPeriodSec > logIntervalSec) {
            logger.info("\n\n");
            logger.info("Word count: " + counter.size());

            publishTopList();
            lastLogTime = now;
        }
    }

    private void publishTopList() {
        // calculate top list:
        SortedMap<Long, String> top = new TreeMap<Long, String>();
        for (Map.Entry<String, Long> entry : counter.entrySet()) {
            long count = entry.getValue();
            String word = entry.getKey();

            top.put(count, word);
            if (top.size() > topListSize) {
                top.remove(top.firstKey());
            }
        }
        
        List<String> sorted_topic = new ArrayList<>();
        Properties props = new Properties();
        props.put("bootstrap.servers", "10.8.128.121:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        Producer<String,String> producer = new KafkaProducer<>(props);

        // Output top list:
        for (Map.Entry<Long, String> entry : top.entrySet()) {
            //logger.info(new StringBuilder("top - ").append(entry.getValue()).append('|').append(entry.getKey()).toString());
            String topic = new StringBuilder("{'wordcount':'").append(entry.getKey().toString()).append("','word':'").append(entry.getValue().toString()).append("'}").toString();
            sorted_topic.add(topic);
            //producer.send(new ProducerRecord<String, String>("test3", entry.getValue(), entry.getValue()));
        }
        logger.info(sorted_topic.toString());

        producer.send(new ProducerRecord<String, String>("test3", "*", sorted_topic.toString()));


        // Clear top list
        long now = System.currentTimeMillis();
        if (now - lastClearTime > clearIntervalSec * 1000) {
            counter.clear();
            lastClearTime = now;
        }
    }
}
