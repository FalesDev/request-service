package co.com.pragma.sqs.sender.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SqsMessageFactory {

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public String toJson(Object payload) {
        return objectMapper.writeValueAsString(payload);
    }

    public Map<String, MessageAttributeValue> buildAttributes(Map<String, String> attributes) {
        return attributes.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> MessageAttributeValue.builder()
                                .stringValue(entry.getValue())
                                .dataType("String")
                                .build()
                ));
    }
}