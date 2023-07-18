package demo.kafka.producer;

import java.util.concurrent.CompletableFuture;

import demo.kafka.properties.KafkaDemoProperties;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KafkaProducerTest {

    private KafkaDemoProperties propertiesMock;
    private KafkaTemplate kafkaTemplateMock;
    private KafkaProducer kafkaClient;

    @BeforeEach
    public void setUp() {
        propertiesMock = mock(KafkaDemoProperties.class);
        kafkaTemplateMock = mock(KafkaTemplate.class);
        kafkaClient = new KafkaProducer(propertiesMock, kafkaTemplateMock);
    }

    /**
     * Ensure the Kafka client is called to emit a message.
     */
    @Test
    public void testProcess_Success() throws Exception {
        String key = "test-key";
        String data = randomUUID().toString();
        String topic = "test-outbound-topic";

        final ProducerRecord<String, String> expectedRecord = new ProducerRecord<>(topic, key, data);

        when(propertiesMock.getOutboundTopic()).thenReturn(topic);
        CompletableFuture<SendResult> futureResultMock = mock(CompletableFuture.class);
        SendResult sendResultMock = mock(SendResult.class);
        when(futureResultMock.get()).thenReturn(sendResultMock);
        when(kafkaTemplateMock.send(any(ProducerRecord.class))).thenReturn(futureResultMock);

        SendResult result = kafkaClient.sendMessage(key, data);

        verify(kafkaTemplateMock, times(1)).send(expectedRecord);
        assertThat(result, equalTo(sendResultMock));
    }

    /**
     * Ensure that an exception thrown on send is percolated up.
     */
    @Test
    public void testProcess_ExceptionOnSend() throws Exception {
        String key = "test-key";
        String data = randomUUID().toString();
        String topic = "test-outbound-topic";

        final ProducerRecord<String, String> expectedRecord = new ProducerRecord<>(topic, key, data);

        when(propertiesMock.getOutboundTopic()).thenReturn(topic);
        doThrow(new RuntimeException("Kafka send failure", new Exception("Failed"))).when(kafkaTemplateMock).send(any(ProducerRecord.class));

        Exception exception = assertThrows(RuntimeException.class, () -> {
                kafkaClient.sendMessage(key, data);
        });

        verify(kafkaTemplateMock, times(1)).send(expectedRecord);
        assertThat(exception.getMessage(), equalTo("Kafka send failure"));
    }
}
