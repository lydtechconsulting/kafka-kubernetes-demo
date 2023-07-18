package demo.kafka.consumer;

import demo.kafka.event.DemoInboundEvent;
import demo.kafka.service.DemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaDemoConsumer {

    final DemoService demoService;

    @KafkaListener(
            topics = "#{'${kafka.inboundTopic}'}",
            groupId = "demo-consumer-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void listen(@Payload DemoInboundEvent event) {
        log.info("Received message - event: " + event);
        try {
            demoService.process(event.getNumberOfEvents());
        } catch (Exception e) {
            log.error("Error processing message: " + e.getMessage());
        }
    }
}
