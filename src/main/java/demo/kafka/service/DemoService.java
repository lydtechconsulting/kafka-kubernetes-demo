package demo.kafka.service;

import demo.kafka.event.DemoOutboundEvent;
import demo.kafka.producer.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DemoService {

    @Autowired
    private final KafkaProducer kafkaProducer;

    /**
     * Sends the requested number of events.
     */
    public void process(Integer numberOfEvents) throws Exception {
        int counter = 0;
        log.info("Sending {} events", numberOfEvents);
        for ( ; counter < numberOfEvents; counter++) {
            sendEvent();
        }
        log.info("Total events sent: {}", counter);
    }

    /**
     * Send an event choosing one of three keys randomly.
     */
    private void sendEvent() throws Exception {
        String key = String.valueOf(RandomUtils.nextInt(1, 4));
        DemoOutboundEvent demoEvent = DemoOutboundEvent.builder()
                .firstName(RandomStringUtils.randomAlphabetic(10))
                .middleName(RandomStringUtils.randomAlphabetic(10))
                .lastName(RandomStringUtils.randomAlphabetic(10))
                .build();
        kafkaProducer.sendMessage(key, demoEvent);
        log.debug("Sent message with key {}.", key);
    }
}
