package co.com.pragma.usecase.processapplicationdecision;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.application.gateways.ApplicationRepository;
import co.com.pragma.model.creditanalysis.ApplicationDecisionMessage;
import co.com.pragma.model.creditanalysis.CreditAnalysisResponsePayload;
import co.com.pragma.model.creditanalysis.PaymentDetail;
import co.com.pragma.model.creditanalysis.PaymentPlanGenerator;
import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.gateways.NotificationGateway;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.model.report.gateways.ReportApprovedGateway;
import co.com.pragma.model.status.Status;
import co.com.pragma.model.status.gateways.StatusRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class ProcessApplicationDecisionUseCase {

    private final ApplicationRepository applicationRepository;
    private final StatusRepository statusRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final NotificationGateway notificationGateway;
    private final PaymentPlanGenerator paymentPlanGenerator;
    private final ReportApprovedGateway reportApprovedGateway;
    private final CustomLogger logger;

    private static final String DECISION_APPROVED = "Approved";

    public Mono<Void> execute(ApplicationDecisionMessage message) {
        return Mono.zip(
                        applicationRepository.findById(message.getApplicationId())
                                .switchIfEmpty(Mono.error(new EntityNotFoundException("Application not found"))),
                        statusRepository.findByNameIgnoreCase(message.getDecision())
                                .switchIfEmpty(Mono.error(new EntityNotFoundException("Status not found for decision")))
                )
                .flatMap(tuple -> {
                    Application application = tuple.getT1();
                    Status newStatus = tuple.getT2();

                    Application.ApplicationBuilder appBuilder = application.toBuilder()
                            .idStatus(newStatus.getId())
                            .updatedAt(LocalDateTime.now());

                    if (DECISION_APPROVED.equalsIgnoreCase(message.getDecision())) {
                        appBuilder.approvedAt(LocalDateTime.now());
                    }

                    Application updatedApp = appBuilder.build();

                    if (DECISION_APPROVED.equalsIgnoreCase(message.getDecision())) {
                        return loanTypeRepository.findById(application.getIdLoanType())
                                .switchIfEmpty(Mono.error(new EntityNotFoundException("LoanType not found for application")))
                                .map(loanType -> paymentPlanGenerator.generate(
                                        updatedApp.getAmount(),
                                        loanType.getInterestRate(),
                                        updatedApp.getTerm()
                                ))
                                .flatMap(paymentPlan -> processAndNotify(updatedApp, newStatus, paymentPlan));
                    } else {
                        return processAndNotify(updatedApp, newStatus, Collections.emptyList());
                    }
                })
                .then();
    }

    private Mono<Void> processAndNotify(Application application, Status status, List<PaymentDetail> paymentPlan) {
        return applicationRepository.save(application)
                .flatMap(updatedApplication -> {
                    CreditAnalysisResponsePayload payload = CreditAnalysisResponsePayload.builder()
                            .applicationId(updatedApplication.getId())
                            .email(updatedApplication.getEmail())
                            .status(status.getName())
                            .amount(updatedApplication.getAmount())
                            .term(updatedApplication.getTerm())
                            .paymentPlan(paymentPlan)
                            .build();

                    logger.trace("Sending notification for applicationId={} with status={}",
                            updatedApplication.getId(), status.getName());
                    return notificationGateway.sendCreditAnalysisDecisionNotification(payload)
                            .then(
                                    DECISION_APPROVED.equalsIgnoreCase(status.getName())
                                            ? reportApprovedGateway.sendReportApprovedCount(updatedApplication, status.getName())
                                            : Mono.empty()
                            );
                });
    }
}
