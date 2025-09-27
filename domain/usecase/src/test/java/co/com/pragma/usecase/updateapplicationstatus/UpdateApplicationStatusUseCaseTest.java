package co.com.pragma.usecase.updateapplicationstatus;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.application.gateways.ApplicationRepository;
import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.gateways.NotificationGateway;
import co.com.pragma.model.report.gateways.ReportApprovedGateway;
import co.com.pragma.model.status.Status;
import co.com.pragma.model.status.gateways.StatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateApplicationStatusUseCaseTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private NotificationGateway notificationGateway;

    @Mock
    private ReportApprovedGateway reportApprovedGateway;

    @Mock
    private CustomLogger customLogger;

    @InjectMocks
    private UpdateApplicationStatusUseCase useCase;

    private Application application;
    private Status approvedStatus;
    private Status rejectedStatus;
    private UUID applicationId;
    private LocalDateTime initialUpdatedAt;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        initialUpdatedAt = LocalDateTime.now().minusDays(1);

        application = Application.builder()
                .id(applicationId)
                .amount(1000.0)
                .term(12)
                .email("test@email.com")
                .idStatus(UUID.randomUUID()) // Estado anterior
                .updatedAt(initialUpdatedAt)
                .approvedAt(null)
                .build();

        approvedStatus = Status.builder()
                .id(UUID.randomUUID())
                .name("Approved")
                .build();

        rejectedStatus = Status.builder()
                .id(UUID.randomUUID())
                .name("Rejected")
                .build();
    }

    @Test
    @DisplayName("Should update application to Approved status successfully")
    void shouldUpdateApplicationToApprovedSuccessfully() {
        when(statusRepository.findByNameIgnoreCase("Approved")).thenReturn(Mono.just(approvedStatus));
        when(applicationRepository.findById(applicationId)).thenReturn(Mono.just(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> {
            Application savedApp = invocation.getArgument(0);
            return Mono.just(savedApp);
        });
        when(notificationGateway.sendDecisionNotification(any(), eq("Approved"))).thenReturn(Mono.empty());
        when(reportApprovedGateway.sendReportApprovedCount(any(), eq("Approved"))).thenReturn(Mono.empty());

        StepVerifier.create(useCase.updateStatus(applicationId, "Approved"))
                .expectNextMatches(updatedApp ->
                        updatedApp.getIdStatus().equals(approvedStatus.getId()) &&
                                updatedApp.getApprovedAt() != null &&
                                updatedApp.getUpdatedAt().isAfter(initialUpdatedAt)
                )
                .verifyComplete();

        verify(notificationGateway).sendDecisionNotification(any(), eq("Approved"));
        verify(reportApprovedGateway).sendReportApprovedCount(any(), eq("Approved"));
        verify(customLogger).info("Starting use case to update request status: {}", applicationId);
        verify(customLogger).trace("Notification + reporting event sent for Application ID: {}", applicationId);
        verify(customLogger).trace("Application status updated successfully for ID: {}", applicationId);
    }

    @Test
    @DisplayName("Should update application to Rejected status successfully")
    void shouldUpdateApplicationToRejectedSuccessfully() {
        when(statusRepository.findByNameIgnoreCase("Rejected")).thenReturn(Mono.just(rejectedStatus));
        when(applicationRepository.findById(applicationId)).thenReturn(Mono.just(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));
        when(notificationGateway.sendDecisionNotification(any(), eq("Rejected"))).thenReturn(Mono.empty());

        StepVerifier.create(useCase.updateStatus(applicationId, "Rejected"))
                .expectNextMatches(updatedApp ->
                        updatedApp.getIdStatus().equals(rejectedStatus.getId()) &&
                                updatedApp.getApprovedAt() == null &&
                                updatedApp.getUpdatedAt().isAfter(initialUpdatedAt)
                )
                .verifyComplete();

        verify(notificationGateway).sendDecisionNotification(any(), eq("Rejected"));
        verifyNoInteractions(reportApprovedGateway);
        verify(customLogger).trace("Notification + reporting event sent for Application ID: {}", applicationId);
    }

    @Test
    @DisplayName("Should throw when status not found")
    void shouldThrowWhenStatusNotFound() {
        when(statusRepository.findByNameIgnoreCase("Invalid")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.updateStatus(applicationId, "Invalid"))
                .expectErrorMatches(throwable ->
                        throwable instanceof EntityNotFoundException &&
                                throwable.getMessage().equals("Invalid status: Invalid")
                )
                .verify();

        verify(customLogger).info("Starting use case to update request status: {}", applicationId);
        verify(customLogger).trace("Application status update failed for ID {}: {}", applicationId, "Invalid status: Invalid");
        verifyNoInteractions(applicationRepository, notificationGateway, reportApprovedGateway);
    }

    @Test
    @DisplayName("Should throw when application not found")
    void shouldThrowWhenApplicationNotFound() {
        when(statusRepository.findByNameIgnoreCase("Approved")).thenReturn(Mono.just(approvedStatus));
        when(applicationRepository.findById(applicationId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.updateStatus(applicationId, "Approved"))
                .expectErrorMatches(throwable ->
                        throwable instanceof EntityNotFoundException &&
                                throwable.getMessage().equals("Application not found")
                )
                .verify();

        verify(customLogger).trace("Application status update failed for ID {}: {}", applicationId, "Application not found");
        verifyNoInteractions(notificationGateway, reportApprovedGateway);
    }

    @Test
    @DisplayName("Should handle report failure gracefully")
    void shouldHandleReportFailure() {
        when(statusRepository.findByNameIgnoreCase("Approved")).thenReturn(Mono.just(approvedStatus));
        when(applicationRepository.findById(applicationId)).thenReturn(Mono.just(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));
        when(notificationGateway.sendDecisionNotification(any(), eq("Approved"))).thenReturn(Mono.empty());
        when(reportApprovedGateway.sendReportApprovedCount(any(), eq("Approved")))
                .thenReturn(Mono.error(new RuntimeException("Report failed")));

        StepVerifier.create(useCase.updateStatus(applicationId, "Approved"))
                .expectError(RuntimeException.class)
                .verify();

        // Verificar que la notificación sí se ejecutó
        verify(notificationGateway).sendDecisionNotification(any(), eq("Approved"));
    }

    @Test
    @DisplayName("Should handle case-insensitive status names")
    void shouldHandleCaseInsensitiveStatusNames() {
        when(statusRepository.findByNameIgnoreCase("approved")).thenReturn(Mono.just(approvedStatus));
        when(applicationRepository.findById(applicationId)).thenReturn(Mono.just(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation ->
                Mono.just(invocation.getArgument(0)));
        when(notificationGateway.sendDecisionNotification(any(), eq("Approved"))).thenReturn(Mono.empty());
        when(reportApprovedGateway.sendReportApprovedCount(any(), eq("Approved"))).thenReturn(Mono.empty());

        StepVerifier.create(useCase.updateStatus(applicationId, "approved"))
                .expectNextCount(1)
                .verifyComplete();

        verify(notificationGateway).sendDecisionNotification(any(), eq("Approved"));
    }
}