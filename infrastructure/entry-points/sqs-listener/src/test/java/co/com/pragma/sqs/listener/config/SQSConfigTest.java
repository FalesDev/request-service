package co.com.pragma.sqs.listener.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.metrics.LoggingMetricPublisher;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class SQSConfigTest {

    private SQSConfig sqsConfig;

    @Mock
    private SqsAsyncClient sqsAsyncClient;

    @Mock
    private SQSProperties sqsProperties;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        sqsConfig = new SQSConfig();

        when(sqsProperties.region()).thenReturn("us-east-1");
        when(sqsProperties.queueUrl()).thenReturn("http://localhost:4566/00000000000/queue-sqs");
        when(sqsProperties.waitTimeSeconds()).thenReturn(20);
        when(sqsProperties.maxNumberOfMessages()).thenReturn(10);
        when(sqsProperties.numberOfThreads()).thenReturn(1);
    }

    @Test
    void listenerSQSClientConfigIsNotNull() {
        var listener = sqsConfig.sqsListener(sqsAsyncClient, sqsProperties, message -> Mono.empty());
        assertThat(listener).isNotNull();
    }

    @Test
    void resolveEndpointIsNullWhenNotConfigured() {
        when(sqsProperties.endpoint()).thenReturn(null);
        assertThat(sqsConfig.resolveEndpoint(sqsProperties)).isNull();
    }

    @Test
    void resolveEndpointReturnsUriWhenConfigured() {
        String endpoint = "http://localhost:4566";
        when(sqsProperties.endpoint()).thenReturn(endpoint);

        URI resolved = sqsConfig.resolveEndpoint(sqsProperties);
        assertThat(resolved).isNotNull();
        assertThat(resolved.toString()).isEqualTo(endpoint);
    }
}
