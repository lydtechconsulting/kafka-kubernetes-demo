package demo.kafka.integration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import demo.kafka.KafkaDemoConfiguration;
import demo.kafka.event.DemoInboundEvent;
import demo.kafka.event.DemoOutboundEvent;
import demo.kafka.rest.api.TriggerEventsRequest;
import demo.kafka.util.TestData;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static demo.kafka.integration.EndToEndIntegrationTest.DEMO_INBOUND_TEST_TOPIC;
import static demo.kafka.integration.EndToEndIntegrationTest.DEMO_OUTBOUND_TEST_TOPIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = { KafkaDemoConfiguration.class } )
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@EmbeddedKafka(controlledShutdown = true, topics = { DEMO_INBOUND_TEST_TOPIC, DEMO_OUTBOUND_TEST_TOPIC })
public class EndToEndIntegrationTest {

    protected final static String DEMO_INBOUND_TEST_TOPIC = "demo-inbound-topic";
    protected final static String DEMO_OUTBOUND_TEST_TOPIC = "demo-outbound-topic";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private KafkaTestListener testReceiver;

    @Autowired
    private TestRestTemplate restTemplate;

    @Configuration
    static class TestConfig {

        @Bean
        public KafkaTestListener testReceiver() {
            return new KafkaTestListener();
        }
    }

    /**
     * Use this receiver to consume messages from the outbound topic.
     */
    public static class KafkaTestListener {
        AtomicInteger counter = new AtomicInteger(0);

        @KafkaListener(groupId = "EndToEndIntegrationTest", topics = DEMO_OUTBOUND_TEST_TOPIC, autoStartup = "true")
        void receive(@Payload final DemoOutboundEvent payload) {
            log.debug("KafkaTestListener - Received message: " + payload);
            counter.incrementAndGet();
        }
    }

    @BeforeEach
    public void setUp() {
        // Wait until the partitions are assigned.
        registry.getListenerContainers().stream().forEach(container ->
                ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic()));
        testReceiver.counter.set(0);
    }

    /**
     * Send in a REST request to trigger emitting multiple outbound events.
     */
    @Test
    public void testSuccess_REST() throws Exception {
        int messagesToTrigger = 10;

        TriggerEventsRequest request = TestData.buildTriggerEventsRequest(messagesToTrigger);

        ResponseEntity<String> response = restTemplate.postForEntity("/v1/demo/trigger", request, String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

        Awaitility.await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testReceiver.counter::get, equalTo(messagesToTrigger));
    }

    /**
     * Send in an inbound event to trigger emitting multiple outbound events.
     */
    @Test
    public void testSuccess_Kafka() throws Exception {
        int messagesToTrigger = 10;

        DemoInboundEvent inboundEvent = TestData.buildDemoInboundEvent(messagesToTrigger);
        kafkaTemplate.send(DEMO_INBOUND_TEST_TOPIC, inboundEvent).get();

        Awaitility.await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testReceiver.counter::get, equalTo(messagesToTrigger));
    }
}
