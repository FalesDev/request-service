package co.com.pragma.usecase.processapplicationdecision;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.application.gateways.ApplicationRepository;
import co.com.pragma.model.creditanalysis.ApplicationDecisionMessage;
import co.com.pragma.model.creditanalysis.PaymentDetail;
import co.com.pragma.model.creditanalysis.PaymentPlanGenerator;
import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.gateways.NotificationGateway;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessApplicationDecisionUseCaseTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private StatusRepository statusRepository;
    @Mock private LoanTypeRepository loanTypeRepository;
    @Mock private NotificationGateway notificationGateway;
    @Mock
    private PaymentPlanGenerator paymentPlanGenerator;
    @Mock private ReportApprovedGateway reportApprovedGateway;
    @Mock private CustomLogger logger;

    @InjectMocks
    private ProcessApplicationDecisionUseCase useCase;

    private UUID applicationId;
    private Application application;
    private Status approvedStatus;
    private Status rejectedStatus;

    @BeforeEach
    void setup() {
        applicationId = UUID.randomUUID();
        application = Application.builder()
                .id(applicationId)
                .email("user@test.com")
                .amount(1000.0)
                .term(12)
                .idLoanType(UUID.randomUUID())
                .build();

        approvedStatus = Status.builder().id(UUID.randomUUID()).name("Approved").build();
        rejectedStatus = Status.builder().id(UUID.randomUUID()).name("Rejected").build();
    }

    @Test
    @DisplayName("Should process approved application successfully")
    void shouldProcessApprovedApplicationSuccessfully() {
        ApplicationDecisionMessage message = new ApplicationDecisionMessage();
        message.setApplicationId(applicationId);
        message.setDecision("Approved");

        when(applicationRepository.findById(applicationId)).thenReturn(Mono.just(application));
        when(statusRepository.findByNameIgnoreCase("Approved")).thenReturn(Mono.just(approvedStatus));
        when(loanTypeRepository.findById(application.getIdLoanType())).thenReturn(Mono.just(
                LoanType.builder().id(application.getIdLoanType()).interestRate(5.0).build()
        ));
        when(paymentPlanGenerator.generate(anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(mock(PaymentDetail.class)));
        when(applicationRepository.save(any())).thenReturn(Mono.just(application));
        when(notificationGateway.sendCreditAnalysisDecisionNotification(any())).thenReturn(Mono.empty());
        when(reportApprovedGateway.sendReportApprovedCount(any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(message))
                .verifyComplete();

        verify(applicationRepository).save(any());
        verify(notificationGateway).sendCreditAnalysisDecisionNotification(any());
        verify(reportApprovedGateway).sendReportApprovedCount(any(), eq("Approved"));
    }


    @Test
    @DisplayName("Should throw when application not found")
    void shouldThrowWhenApplicationNotFound() {
        ApplicationDecisionMessage message = new ApplicationDecisionMessage();
        message.setApplicationId(applicationId);
        message.setDecision("Approved");

        when(applicationRepository.findById(applicationId)).thenReturn(Mono.empty());
        when(statusRepository.findByNameIgnoreCase("Approved")).thenReturn(Mono.just(approvedStatus));

        StepVerifier.create(useCase.execute(message))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(applicationRepository).findById(applicationId);
        verify(statusRepository).findByNameIgnoreCase("Approved");
        verifyNoInteractions(loanTypeRepository, notificationGateway, reportApprovedGateway);
    }

    @Test
    @DisplayName("Should process rejected application without calling loanType or report")
    void shouldProcessRejectedApplication() {
        ApplicationDecisionMessage message = new ApplicationDecisionMessage();
        message.setApplicationId(applicationId);
        message.setDecision("Rejected");

        when(applicationRepository.findById(applicationId)).thenReturn(Mono.just(application));
        when(statusRepository.findByNameIgnoreCase("Rejected")).thenReturn(Mono.just(rejectedStatus));
        when(applicationRepository.save(any())).thenReturn(Mono.just(application));
        when(notificationGateway.sendCreditAnalysisDecisionNotification(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(message))
                .verifyComplete();

        verify(applicationRepository).save(any());
        verify(notificationGateway).sendCreditAnalysisDecisionNotification(any());
        verifyNoInteractions(loanTypeRepository, paymentPlanGenerator, reportApprovedGateway);
    }
}
