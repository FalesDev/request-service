package co.com.pragma.sqs.sender.adapter;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.report.ReportApprovedMessage;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportApprovedAdapterTest {

    @Mock
    private SQSSender sqsSender;

    @Mock
    private SqsMessageFactory messageFactory;

    @Captor
    private ArgumentCaptor<ReportApprovedMessage> messageCaptor;

    @Captor
    private ArgumentCaptor<Map<String, String>> attributesCaptor;

    private ReportApprovedAdapter reportApprovedAdapter;

    private final String reportingQueue = "reporting-queue";
    private Application application;
    private final UUID applicationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reportApprovedAdapter = new ReportApprovedAdapter(sqsSender, messageFactory, reportingQueue);

        application = Application.builder()
                .id(applicationId)
                .amount(20000.0)
                .term(12)
                .email("test@example.com")
                .idDocument("99999999")
                .idStatus(UUID.randomUUID())
                .idLoanType(UUID.randomUUID())
                .idUser(UUID.randomUUID())
                .build();
    }

    @Test
    @DisplayName("Should send report approved message successfully")
    void sendReportApprovedCount_ShouldSendMessage() {
        String status = "APPROVED";
        String expectedJson = "{\"applicationId\":\"" + applicationId + "\",\"amount\":20000.0,\"state\":\"APPROVED\"}";
        Map<String, MessageAttributeValue> expectedAttributes = Map.of(
                "key", MessageAttributeValue.builder().dataType("String").stringValue("value").build()
        );

        when(messageFactory.toJson(any(ReportApprovedMessage.class))).thenReturn(expectedJson);
        when(messageFactory.buildAttributes(any(Map.class))).thenReturn(expectedAttributes);
        when(sqsSender.send(eq(reportingQueue), eq(expectedJson), eq(expectedAttributes)))
                .thenReturn(Mono.just("message-id"));

        StepVerifier.create(reportApprovedAdapter.sendReportApprovedCount(application, status))
                .verifyComplete();

        verify(messageFactory).toJson(messageCaptor.capture());
        ReportApprovedMessage capturedMessage = messageCaptor.getValue();

        assertEquals(applicationId, capturedMessage.getApplicationId());
        assertEquals(20000.0, capturedMessage.getAmount());
        assertEquals("APPROVED", capturedMessage.getState());

        verify(messageFactory).buildAttributes(attributesCaptor.capture());
        Map<String, String> capturedAttributes = attributesCaptor.getValue();

        assertEquals("REPORT_APPROVED", capturedAttributes.get("eventType"));
        assertEquals("total_approved_requests", capturedAttributes.get("reportId"));

        verify(sqsSender).send(reportingQueue, expectedJson, expectedAttributes);
    }

    @Test
    @DisplayName("Should handle different status values")
    void sendReportApprovedCount_ShouldHandleDifferentStatus() {
        String status = "REJECTED";
        String expectedJson = "json-payload";
        Map<String, MessageAttributeValue> expectedAttributes = Map.of();

        when(messageFactory.toJson(any(ReportApprovedMessage.class))).thenReturn(expectedJson);
        when(messageFactory.buildAttributes(any(Map.class))).thenReturn(expectedAttributes);
        when(sqsSender.send(eq(reportingQueue), eq(expectedJson), eq(expectedAttributes)))
                .thenReturn(Mono.just("message-id"));

        StepVerifier.create(reportApprovedAdapter.sendReportApprovedCount(application, status))
                .verifyComplete();

        verify(messageFactory).toJson(messageCaptor.capture());
        ReportApprovedMessage capturedMessage = messageCaptor.getValue();
        assertEquals("REJECTED", capturedMessage.getState());
    }

    @Test
    @DisplayName("Should handle SQS send errors")
    void sendReportApprovedCount_ShouldHandleSqsErrors() {
        // Arrange
        String status = "APPROVED";
        String expectedJson = "json-payload";
        Map<String, MessageAttributeValue> expectedAttributes = Map.of();

        when(messageFactory.toJson(any(ReportApprovedMessage.class))).thenReturn(expectedJson);
        when(messageFactory.buildAttributes(any(Map.class))).thenReturn(expectedAttributes);
        when(sqsSender.send(eq(reportingQueue), eq(expectedJson), eq(expectedAttributes)))
                .thenReturn(Mono.error(new RuntimeException("SQS error")));

        // Act & Assert
        StepVerifier.create(reportApprovedAdapter.sendReportApprovedCount(application, status))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("SQS error"))
                .verify();
    }

    @Test
    @DisplayName("Should build correct message payload")
    void sendReportApprovedCount_ShouldBuildCorrectPayload() {
        // Arrange
        String status = "PENDING";
        String expectedJson = "json-payload";
        Map<String, MessageAttributeValue> expectedAttributes = Map.of();

        when(messageFactory.toJson(any(ReportApprovedMessage.class))).thenReturn(expectedJson);
        when(messageFactory.buildAttributes(any(Map.class))).thenReturn(expectedAttributes);
        when(sqsSender.send(eq(reportingQueue), eq(expectedJson), eq(expectedAttributes)))
                .thenReturn(Mono.just("message-id"));

        StepVerifier.create(reportApprovedAdapter.sendReportApprovedCount(application, status))
                .verifyComplete();

        verify(messageFactory).toJson(messageCaptor.capture());
        ReportApprovedMessage capturedMessage = messageCaptor.getValue();

        assertAll(
                () -> assertEquals(application.getId(), capturedMessage.getApplicationId()),
                () -> assertEquals(application.getAmount(), capturedMessage.getAmount()),
                () -> assertEquals(status, capturedMessage.getState())
        );
    }
}