package demo.kafka.producer;

import demo.kafka.properties.KafkaDemoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {
    @Autowired
    private final KafkaDemoProperties properties;

    @Autowired
    private final KafkaTemplate kafkaTemplate;

    public SendResult sendMessage(String key, Object payload) throws Exception {
        final ProducerRecord<String, Object> record = new ProducerRecord<>(properties.getOutboundTopic(), key, payload);
        return (SendResult) kafkaTemplate.send(record).get();
    }
}
