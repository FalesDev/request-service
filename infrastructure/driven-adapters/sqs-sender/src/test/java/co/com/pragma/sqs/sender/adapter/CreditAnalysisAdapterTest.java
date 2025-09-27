package co.com.pragma.sqs.sender.adapter;

import co.com.pragma.model.creditanalysis.CreditAnalysisPayload;
import co.com.pragma.sqs.sender.SQSSender;
import co.com.pragma.sqs.sender.factory.SqsMessageFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditAnalysisAdapterTest {

    @Mock
    private SQSSender sqsSender;

    @Mock
    private SqsMessageFactory messageFactory;

    private CreditAnalysisAdapter creditAnalysisAdapter;

    private final String indebtednessQueue = "indebtedness-queue";
    private CreditAnalysisPayload payload;
    private final UUID applicationId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        creditAnalysisAdapter = new CreditAnalysisAdapter(sqsSender, messageFactory, indebtednessQueue);

        payload = CreditAnalysisPayload.builder()
                .idApplication(applicationId)
                .idUser(userId)
                .build();
    }

    @Test
    @DisplayName("Should send credit analysis request successfully")
    void requestAnalysis_ShouldSendMessage() {
        String jsonMessage = "{\"idApplication\":\"" + applicationId + "\",\"idUser\":\"" + userId + "\"}";
        Map<String, MessageAttributeValue> expectedAttributes = Map.of(
                "key", MessageAttributeValue.builder().dataType("String").stringValue("value").build()
        );

        when(messageFactory.toJson(payload)).thenReturn(jsonMessage);
        when(messageFactory.buildAttributes(any())).thenReturn(expectedAttributes);
        when(sqsSender.send(eq(indebtednessQueue), eq(jsonMessage), eq(expectedAttributes)))
                .thenReturn(Mono.just("message-id"));

        StepVerifier.create(creditAnalysisAdapter.requestAnalysis(payload))
                .verifyComplete();

        verify(messageFactory).toJson(payload);
        verify(messageFactory).buildAttributes(Map.of(
                "eventType", "CREDIT_ANALYSIS_REQUESTED",
                "applicationId", applicationId.toString(),
                "idUser", userId.toString()
        ));
        verify(sqsSender).send(indebtednessQueue, jsonMessage, expectedAttributes);
    }

    @Test
    @DisplayName("Should handle SQS send error")
    void requestAnalysis_ShouldHandleError() {
        String jsonMessage = "json-payload";
        Map<String, MessageAttributeValue> expectedAttributes = Map.of();

        when(messageFactory.toJson(payload)).thenReturn(jsonMessage);
        when(messageFactory.buildAttributes(any())).thenReturn(expectedAttributes);
        when(sqsSender.send(eq(indebtednessQueue), eq(jsonMessage), eq(expectedAttributes)))
                .thenReturn(Mono.error(new RuntimeException("SQS error")));

        StepVerifier.create(creditAnalysisAdapter.requestAnalysis(payload))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("SQS error"))
                .verify();

        verify(sqsSender).send(indebtednessQueue, jsonMessage, expectedAttributes);
    }

    @Test
    @DisplayName("Should build correct attributes map")
    void requestAnalysis_ShouldBuildCorrectAttributes() {
        String jsonMessage = "json-payload";
        Map<String, MessageAttributeValue> expectedAttributes = Map.of(
                "eventType", MessageAttributeValue.builder().dataType("String").stringValue("CREDIT_ANALYSIS_REQUESTED").build(),
                "applicationId", MessageAttributeValue.builder().dataType("String").stringValue(applicationId.toString()).build(),
                "idUser", MessageAttributeValue.builder().dataType("String").stringValue(userId.toString()).build()
        );

        when(messageFactory.toJson(payload)).thenReturn(jsonMessage);
        when(messageFactory.buildAttributes(any(Map.class))).thenReturn(expectedAttributes);
        when(sqsSender.send(eq(indebtednessQueue), eq(jsonMessage), eq(expectedAttributes)))
                .thenReturn(Mono.just("message-id"));

        StepVerifier.create(creditAnalysisAdapter.requestAnalysis(payload))
                .verifyComplete();

        verify(messageFactory).buildAttributes(Map.of(
                "eventType", "CREDIT_ANALYSIS_REQUESTED",
                "applicationId", applicationId.toString(),
                "idUser", userId.toString()
        ));
    }
}
