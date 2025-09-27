package co.com.pragma.sqs.sender.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SQSSenderConfigTest {

    private SQSSenderConfig config;

    @Mock
    private MetricPublisher publisher;

    private SQSSenderProperties properties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        config = new SQSSenderConfig();
        properties = new SQSSenderProperties("us-east-1", Map.of("queue1", "url1"));
    }

    @Test
    @DisplayName("Should create SqsAsyncClient with given region and metric publisher")
    void shouldCreateSqsAsyncClient() {
        SqsAsyncClient client = config.configSqs(properties, publisher);

        assertThat(client).isNotNull();
        assertThat(client.serviceClientConfiguration().region())
                .isEqualTo(Region.US_EAST_1);
    }

    @Test
    @DisplayName("Should build AwsCredentialsProviderChain inside configuration")
    void shouldBuildCredentialsProviderChain() {
        var providerChain = config.configSqs(properties, publisher)
                .serviceClientConfiguration()
                .credentialsProvider();

        assertThat(providerChain).isNotNull();
    }
}