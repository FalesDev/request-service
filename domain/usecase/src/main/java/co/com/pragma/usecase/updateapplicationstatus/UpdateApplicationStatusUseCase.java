package co.com.pragma.usecase.updateapplicationstatus;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.application.gateways.ApplicationRepository;
import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.gateways.NotificationGateway;
import co.com.pragma.model.report.gateways.ReportApprovedGateway;
import co.com.pragma.model.status.gateways.StatusRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class UpdateApplicationStatusUseCase {

    private final ApplicationRepository applicationRepository;
    private final StatusRepository statusRepository;
    private final NotificationGateway notificationGateway;
    private final ReportApprovedGateway reportApprovedGateway;
    private final CustomLogger customLogger;

    private static final String APPROVED_STATUS_NAME = "Approved";

    public Mono<Application> updateStatus(UUID applicationId, String newStatusName) {
        customLogger.info("Starting use case to update request status: {}", applicationId);

        return statusRepository.findByNameIgnoreCase(newStatusName)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Invalid status: " + newStatusName)))
                .flatMap(newStatus -> applicationRepository.findById(applicationId)
                        .switchIfEmpty(Mono.error(new EntityNotFoundException("Application not found")))
                        .flatMap(application -> {
                            application.setIdStatus(newStatus.getId());
                            application.setUpdatedAt(LocalDateTime.now());
                            if (APPROVED_STATUS_NAME.equalsIgnoreCase(newStatus.getName())) {
                                application.setApprovedAt(LocalDateTime.now());
                            }else {
                                application.setApprovedAt(null);
                            }
                            return applicationRepository.save(application)
                                    .flatMap(savedApp ->
                                            notificationGateway.sendDecisionNotification(savedApp, newStatus.getName())
                                                    .then(
                                                            APPROVED_STATUS_NAME.equalsIgnoreCase(newStatus.getName())
                                                                    ? reportApprovedGateway.sendReportApprovedCount(savedApp, newStatus.getName())
                                                                    : Mono.empty()
                                                    )
                                                    .then(Mono.fromRunnable(() ->
                                                            customLogger.trace("Notification + reporting event sent for Application ID: {}", savedApp.getId())
                                                    ))
                                                    .thenReturn(savedApp)
                                    );
                        })
                )
                .doOnSuccess(updatedApp ->
                        customLogger.trace("Application status updated successfully for ID: {}", updatedApp.getId())
                )
                .doOnError(error ->
                        customLogger.trace("Application status update failed for ID {}: {}", applicationId, error.getMessage())
                );
    }
}
