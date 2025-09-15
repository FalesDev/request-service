package co.com.pragma.sqs.sender;

import co.com.pragma.sqs.sender.config.SQSSenderProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class SQSSender /*implements SomeGateway*/ {
    private final SQSSenderProperties properties;
    private final SqsAsyncClient client;

    public Mono<String> send(String queueName, String message) {
        return send(queueName, message, null);
    }

    public Mono<String> send(String queueName, String message, Map<String, MessageAttributeValue> attributes) {
        String queueUrl = properties.queues().get(queueName);
        if (queueUrl == null) {
            return Mono.error(new IllegalArgumentException("Queue not configured: " + queueName));
        }
        return Mono.fromCallable(() -> buildRequest(message, queueUrl, attributes))
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .doOnNext(response -> log.debug("Message sent to {} with id {}", queueName, response.messageId()))
                .map(SendMessageResponse::messageId);
    }

    private SendMessageRequest buildRequest(String message, String queueUrl, Map<String, MessageAttributeValue> attributes) {
        SendMessageRequest.Builder builder = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(message);

        if (attributes != null && !attributes.isEmpty()) {
            builder.messageAttributes(attributes);
        }

        return builder.build();
    }
}
