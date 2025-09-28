package co.com.pragma.sqs.listener.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
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

    @Test
    void sqsPropertiesRecordWorksAsExpected() {
        SQSProperties props = new SQSProperties(
                "us-east-1",
                "http://localhost:4566",
                "http://localhost:4566/00000000000/queue-sqs",
                20,
                30,
                10,
                2
        );

        assertThat(props.region()).isEqualTo("us-east-1");
        assertThat(props.endpoint()).isEqualTo("http://localhost:4566");
        assertThat(props.queueUrl()).contains("queue-sqs");
        assertThat(props.waitTimeSeconds()).isEqualTo(20);
        assertThat(props.visibilityTimeoutSeconds()).isEqualTo(30);
        assertThat(props.maxNumberOfMessages()).isEqualTo(10);
        assertThat(props.numberOfThreads()).isEqualTo(2);
        assertThat(props.toString()).contains("us-east-1");
    }

    @Test
    void getProviderChainReturnsNonNullChain() throws Exception {
        var method = SQSConfig.class.getDeclaredMethod("getProviderChain");
        method.setAccessible(true);

        Object chain = method.invoke(sqsConfig);

        assertThat(chain).isNotNull();
        assertThat(chain.getClass().getSimpleName()).contains("AwsCredentialsProviderChain");
    }

    @Test
    void sqsListenerProcessesMessageWithMono() {
        var listener = sqsConfig.sqsListener(
                sqsAsyncClient,
                sqsProperties,
                msg -> Mono.fromRunnable(() -> assertThat(msg).isNotNull())
        );
        assertThat(listener).isNotNull();
    }
}
