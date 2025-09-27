package co.com.pragma.sqs.sender;

import co.com.pragma.sqs.sender.config.SQSSenderProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SQSSenderTest {

    private SQSSender sender;

    @Mock
    private SqsAsyncClient sqsClient;

    @BeforeEach
    void setUp() {
        SQSSenderProperties properties = new SQSSenderProperties(
                "us-east-1",
                Map.of("myQueue", "http://sqs.amazonaws.com/123/myQueue")
        );
        sender = new SQSSender(properties, sqsClient);
    }

    @Test
    @DisplayName("Should send message successfully and return messageId")
    void shouldSendMessageSuccessfully() {
        String expectedId = UUID.randomUUID().toString();
        SendMessageResponse response = SendMessageResponse.builder()
                .messageId(expectedId)
                .build();

        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        StepVerifier.create(sender.send("myQueue", "Hello World"))
                .expectNext(expectedId)
                .verifyComplete();

        verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    @DisplayName("Should include attributes when provided")
    void shouldIncludeAttributes() {
        String expectedId = "msg-123";
        Map<String, MessageAttributeValue> attributes =
                Map.of("attr1", MessageAttributeValue.builder().stringValue("value1").dataType("String").build());

        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        SendMessageResponse.builder().messageId(expectedId).build()
                ));

        StepVerifier.create(sender.send("myQueue", "With attributes", attributes))
                .expectNext(expectedId)
                .verifyComplete();

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());

        SendMessageRequest request = captor.getValue();
        assertThat(request.messageAttributes()).containsKey("attr1");
        assertThat(request.messageAttributes().get("attr1").stringValue()).isEqualTo("value1");
    }

    @Test
    @DisplayName("Should return error when queue name is not configured")
    void shouldReturnErrorWhenQueueNotConfigured() {
        StepVerifier.create(sender.send("unknownQueue", "Hello"))
                .expectErrorSatisfies(error ->
                        assertThat(error)
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Queue not configured"))
                .verify();
    }

    @Test
    @DisplayName("Should handle empty attributes map without sending attributes")
    void shouldHandleEmptyAttributes() {
        String expectedId = "msg-456";

        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        SendMessageResponse.builder().messageId(expectedId).build()
                ));

        StepVerifier.create(sender.send("myQueue", "Empty attributes", Map.of()))
                .expectNext(expectedId)
                .verifyComplete();

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());

        SendMessageRequest request = captor.getValue();
        assertThat(request.messageAttributes()).isEmpty();
    }
}
