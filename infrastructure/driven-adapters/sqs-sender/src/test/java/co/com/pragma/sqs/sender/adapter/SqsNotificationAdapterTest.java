package co.com.pragma.sqs.sender.adapter;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.creditanalysis.CreditAnalysisResponsePayload;
import co.com.pragma.sqs.sender.SQSSender;
import co.com.pragma.sqs.sender.factory.SqsMessageFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsNotificationAdapterTest {

    @Mock
    private SQSSender sqsSender;

    @Mock
    private SqsMessageFactory messageFactory;

    private SqsNotificationAdapter adapter;

    private final String notificationsQueue = "notifications-queue";

    private Application application;
    private final UUID appId = UUID.randomUUID();

    @Captor
    private ArgumentCaptor<Map<String, Object>> payloadCaptor;

    @BeforeEach
    void setUp() {
        adapter = new SqsNotificationAdapter(sqsSender, messageFactory, notificationsQueue);

        application = Application.builder()
                .id(appId)
                .email("user@test.com")
                .amount(1000.0)
                .term(6)
                .idUser(UUID.randomUUID())
                .idDocument("12345678")
                .idStatus(UUID.randomUUID())
                .idLoanType(UUID.randomUUID())
                .build();
    }

    @Test
    @DisplayName("Should send decision notification successfully")
    void sendDecisionNotification_ShouldSendMessage() {
        String status = "APPROVED";
        String expectedJson = "json-payload";
        Map<String, MessageAttributeValue> expectedAttributes = Map.of();

        when(messageFactory.toJson(any())).thenReturn(expectedJson);
        when(messageFactory.buildAttributes(any())).thenReturn(expectedAttributes);
        when(sqsSender.send(eq(notificationsQueue), eq(expectedJson), eq(expectedAttributes)))
                .thenReturn(Mono.just("message-id"));

        StepVerifier.create(adapter.sendDecisionNotification(application, status))
                .verifyComplete();

        verify(messageFactory).toJson(payloadCaptor.capture());
        Map<String, Object> capturedPayload = payloadCaptor.getValue();

        assertThat(capturedPayload)
                .containsEntry("applicationId", appId)
                .containsEntry("email", "user@test.com")
                .containsEntry("status", "APPROVED")
                .containsEntry("amount", 1000.0)
                .containsEntry("term", 6);

        verify(messageFactory).buildAttributes(Map.of(
                "eventType", "DECISION_FINAL",
                "applicationId", appId.toString()
        ));
        verify(sqsSender).send(notificationsQueue, expectedJson, expectedAttributes);
    }

    @Test
    @DisplayName("Should send credit analysis decision notification successfully")
    void sendCreditAnalysisDecisionNotification_ShouldSendMessage() {
        CreditAnalysisResponsePayload payload = new CreditAnalysisResponsePayload();
        payload.setApplicationId(appId);

        String expectedJson = "json-credit-analysis";
        Map<String, MessageAttributeValue> expectedAttributes = Map.of();

        when(messageFactory.toJson(payload)).thenReturn(expectedJson);
        when(messageFactory.buildAttributes(any())).thenReturn(expectedAttributes);
        when(sqsSender.send(eq(notificationsQueue), eq(expectedJson), eq(expectedAttributes)))
                .thenReturn(Mono.just("message-id"));

        StepVerifier.create(adapter.sendCreditAnalysisDecisionNotification(payload))
                .verifyComplete();

        verify(messageFactory).toJson(payload);
        verify(messageFactory).buildAttributes(Map.of(
                "eventType", "CREDIT_ANALYSIS_RESPONSE",
                "applicationId", appId.toString()
        ));
        verify(sqsSender).send(notificationsQueue, expectedJson, expectedAttributes);
    }

    @Test
    @DisplayName("Should fail if Application is null in decision notification")
    void sendDecisionNotification_ShouldFailOnNullApplication() {
        assertThrows(NullPointerException.class, () ->
                adapter.sendDecisionNotification(null, "APPROVED")
        );
    }
}
