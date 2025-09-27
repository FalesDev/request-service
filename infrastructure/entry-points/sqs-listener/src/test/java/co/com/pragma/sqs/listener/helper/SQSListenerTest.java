package co.com.pragma.sqs.listener.helper;

import co.com.pragma.sqs.listener.config.SQSProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SQSListenerTest {

    @Mock
    private SqsAsyncClient asyncClient;

    @Mock
    private SQSProperties sqsProperties;

    @Mock
    private Function<Message, Mono<Void>> mockProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(sqsProperties.queueUrl()).thenReturn("http://localhost:4566/00000000000/queueName");
        when(sqsProperties.maxNumberOfMessages()).thenReturn(10);
        when(sqsProperties.waitTimeSeconds()).thenReturn(20);
        when(sqsProperties.visibilityTimeoutSeconds()).thenReturn(30);
        when(sqsProperties.numberOfThreads()).thenReturn(1);

        var message = Message.builder()
                .body("message")
                .receiptHandle("test-receipt-handle")
                .build();
        var deleteMessageResponse = DeleteMessageResponse.builder().build();
        var messageResponse = ReceiveMessageResponse.builder().messages(message).build();

        when(asyncClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(messageResponse));
        when(asyncClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(deleteMessageResponse));

        when(mockProcessor.apply(any(Message.class))).thenReturn(Mono.empty());
    }

    @Test
    void listenerTest() {
        var sqsListener = SQSListener.builder()
                .client(asyncClient)
                .properties(sqsProperties)
                .processor(mockProcessor)
                .operation("operation")
                .build();

        Flux<Void> flow = ReflectionTestUtils.invokeMethod(sqsListener, "listen");
        StepVerifier.create(flow).verifyComplete();
    }
}
