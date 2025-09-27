package co.com.pragma.sqs.sender.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SqsMessageFactoryTest {

    private SqsMessageFactory factory;

    @BeforeEach
    void setUp() {
        factory = new SqsMessageFactory(new ObjectMapper());
    }

    @Test
    @DisplayName("Should serialize object to JSON")
    void shouldSerializeObjectToJson() {
        record TestPayload(String name, int age) {}
        TestPayload payload = new TestPayload("Fabricio", 25);

        String json = factory.toJson(payload);

        assertThat(json).contains("\"name\":\"Fabricio\"");
        assertThat(json).contains("\"age\":25");
    }

    @Test
    @DisplayName("Should build SQS attributes from map")
    void shouldBuildAttributes() {
        Map<String, String> input = Map.of("key1", "value1", "key2", "value2");

        Map<String, MessageAttributeValue> result = factory.buildAttributes(input);

        assertThat(result).hasSize(2);
        assertThat(result.get("key1").stringValue()).isEqualTo("value1");
        assertThat(result.get("key1").dataType()).isEqualTo("String");
        assertThat(result.get("key2").stringValue()).isEqualTo("value2");
    }

    @Test
    @DisplayName("Should return empty map when input attributes are empty")
    void shouldReturnEmptyMap() {
        Map<String, MessageAttributeValue> result = factory.buildAttributes(Map.of());

        assertThat(result).isEmpty();
    }
}
