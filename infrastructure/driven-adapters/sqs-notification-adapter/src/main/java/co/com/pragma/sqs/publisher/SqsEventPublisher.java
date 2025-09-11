package co.com.pragma.sqs.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SqsEventPublisher {

    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;

    @Value("${cloud.aws.end-point.uri}")
    private String queueUrl;

    public <T> Mono<Void> publishEvent(String eventType, T payload, Map<String, String> extraAttributes) {
        return Mono.fromCallable(() -> buildMessage(payload))
                .flatMap(messageBody -> {
                    Map<String, MessageAttributeValue> attributes = buildAttributes(eventType, extraAttributes);

                    SendMessageRequest request = SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageBody(messageBody)
                            .messageAttributes(attributes)
                            .build();

                    return Mono.fromFuture(sqsAsyncClient.sendMessage(request));
                })
                .then();
    }

    @SneakyThrows
    private <T> String buildMessage(T payload) {
        return objectMapper.writeValueAsString(payload);
    }

    private Map<String, MessageAttributeValue> buildAttributes(String eventType, Map<String, String> extraAttributes) {
        Map<String, MessageAttributeValue> attributes = new HashMap<>();

        attributes.put("eventType", MessageAttributeValue.builder()
                .stringValue(eventType)
                .dataType("String")
                .build());

        if (extraAttributes != null) {
            extraAttributes.forEach((key, value) ->
                    attributes.put(key, MessageAttributeValue.builder()
                            .stringValue(value)
                            .dataType("String")
                            .build())
            );
        }

        return attributes;
    }
}
