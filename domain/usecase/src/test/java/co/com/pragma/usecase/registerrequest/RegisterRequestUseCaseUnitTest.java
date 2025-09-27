package co.com.pragma.usecase.registerrequest;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.application.gateways.ApplicationRepository;
import co.com.pragma.model.auth.ValidatedUser;
import co.com.pragma.model.auth.gateway.AuthValidationGateway;
import co.com.pragma.model.creditanalysis.CreditAnalysisPayload;
import co.com.pragma.model.creditanalysis.gateway.CreditAnalysisGateway;
import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.exception.InvalidAmountException;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.gateways.TransactionManager;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.model.status.Status;
import co.com.pragma.model.status.gateways.StatusRepository;
import co.com.pragma.usecase.findloantypebyid.FindLoanTypeByIdUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegisterRequestUseCaseUnitTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private StatusRepository statusRepository;
    @Mock
    private LoanTypeRepository loanTypeRepository;
    @Mock
    private TransactionManager transactionManager;
    @Mock
    private AuthValidationGateway authValidationGateway;
    @Mock
    private FindLoanTypeByIdUseCase findLoanTypeByIdUseCase;
    @Mock
    private CreditAnalysisGateway creditAnalysisGateway;
    @Mock
    private CustomLogger customLogger;

    @InjectMocks
    private RegisterRequestUseCase registerRequestUseCase;

    private Application testApplication;
    private LoanType loanType;
    private Status status;
    private String token;
    private ValidatedUser user;

    @BeforeEach
    void setUp() {
        token = "valid-token";
        UUID userId = UUID.randomUUID();
        UUID loanTypeId = UUID.randomUUID();

        user = ValidatedUser.builder()
                .idUser(userId)
                .email("FABRICIO@GMAIL.COM")
                .idDocument("77777777")
                .baseSalary(50000.0)
                .build();

        testApplication = Application.builder()
                .id(UUID.randomUUID())
                .amount(20000.0)
                .term(12)
                .idDocument("77777777")
                .idLoanType(loanTypeId)
                .build();

        loanType = LoanType.builder()
                .id(loanTypeId)
                .name("Car Loan")
                .minAmount(10000.00)
                .maxAmount(49999.99)
                .interestRate(10.2)
                .automaticValidation(true)
                .build();

        status = Status.builder()
                .id(UUID.randomUUID())
                .name("Pending Review")
                .description("The application was created and is waiting for review.")
                .build();
    }

    @Test
    @DisplayName("Should register and enqueue for analysis when automatic validation is true and no active loans")
    void registerApplicationSuccess_WithAutomaticValidation() {
        when(transactionManager.executeInTransaction(any())).thenAnswer(invocation -> invocation.getArgument(0, Mono.class));
        when(authValidationGateway.validateClientUser(testApplication.getIdDocument(), token)).thenReturn(Mono.just(user));
        when(findLoanTypeByIdUseCase.findById(testApplication.getIdLoanType())).thenReturn(Mono.just(loanType));
        when(applicationRepository.findActiveLoansByIdUser(user.getIdUser())).thenReturn(Flux.empty()); // Sin préstamos activos
        when(statusRepository.findByName("Pending Review")).thenReturn(Mono.just(status));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application app = inv.getArgument(0);
            app.setId(UUID.randomUUID());
            return Mono.just(app);
        });
        when(creditAnalysisGateway.requestAnalysis(any(CreditAnalysisPayload.class))).thenReturn(Mono.empty());

        StepVerifier.create(registerRequestUseCase.registerApplication(testApplication, token))
                .expectNextMatches(app ->
                        app.getEmail().equals("fabricio@gmail.com") &&
                                app.getIdStatus().equals(status.getId()) &&
                                app.getIdUser().equals(user.getIdUser())
                )
                .verifyComplete();

        verify(authValidationGateway).validateClientUser(testApplication.getIdDocument(), token);
        verify(findLoanTypeByIdUseCase).findById(testApplication.getIdLoanType());
        verify(applicationRepository).findActiveLoansByIdUser(user.getIdUser());
        verify(statusRepository).findByName("Pending Review");
        verify(applicationRepository).save(any(Application.class));
        verify(creditAnalysisGateway).requestAnalysis(any(CreditAnalysisPayload.class));
    }

    @Test
    @DisplayName("Should register but not enqueue when automatic validation is false")
    void registerApplicationSuccess_WithoutAutomaticValidation() {
        loanType.setAutomaticValidation(false);
        when(transactionManager.executeInTransaction(any())).thenAnswer(invocation -> invocation.getArgument(0, Mono.class));
        when(authValidationGateway.validateClientUser(testApplication.getIdDocument(), token)).thenReturn(Mono.just(user));
        when(findLoanTypeByIdUseCase.findById(testApplication.getIdLoanType())).thenReturn(Mono.just(loanType));
        when(applicationRepository.findActiveLoansByIdUser(user.getIdUser())).thenReturn(Flux.empty());
        when(statusRepository.findByName("Pending Review")).thenReturn(Mono.just(status));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(registerRequestUseCase.registerApplication(testApplication, token))
                .expectNextMatches(app -> app.getIdStatus().equals(status.getId()))
                .verifyComplete();

        verify(creditAnalysisGateway, never()).requestAnalysis(any());
    }

    @Test
    @DisplayName("Should build payload correctly when user has active loans")
    void registerApplication_WithExistingActiveLoans() {
        LoanType existingLoanType = LoanType.builder().id(UUID.randomUUID()).interestRate(5.0).name("Personal Loan").build();
        Application existingLoan = Application.builder().id(UUID.randomUUID()).idLoanType(existingLoanType.getId()).amount(5000.0).term(6).build();
        when(transactionManager.executeInTransaction(any())).thenAnswer(invocation -> invocation.getArgument(0, Mono.class));
        when(authValidationGateway.validateClientUser(testApplication.getIdDocument(), token)).thenReturn(Mono.just(user));
        when(findLoanTypeByIdUseCase.findById(testApplication.getIdLoanType())).thenReturn(Mono.just(loanType));
        when(applicationRepository.findActiveLoansByIdUser(user.getIdUser())).thenReturn(Flux.just(existingLoan));
        when(loanTypeRepository.findByIds(List.of(existingLoan.getIdLoanType()))).thenReturn(Flux.just(existingLoanType));
        when(statusRepository.findByName("Pending Review")).thenReturn(Mono.just(status));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(creditAnalysisGateway.requestAnalysis(any(CreditAnalysisPayload.class))).thenReturn(Mono.empty());

        StepVerifier.create(registerRequestUseCase.registerApplication(testApplication, token))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<CreditAnalysisPayload> payloadCaptor = ArgumentCaptor.forClass(CreditAnalysisPayload.class);
        verify(creditAnalysisGateway).requestAnalysis(payloadCaptor.capture());
        CreditAnalysisPayload capturedPayload = payloadCaptor.getValue();

        assertThat(capturedPayload.getLoanAssets()).hasSize(1);
        assertThat(capturedPayload.getLoanAssets().getFirst().getAmount()).isEqualTo(5000.0);
        assertThat(capturedPayload.getLoanAssets().getFirst().getEstado()).isEqualTo("Approved");
        assertThat(capturedPayload.getNewLoanDetails().getEstado()).isEqualTo("Pending Review");
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when loan type is not found")
    void registerApplicationLoanTypeNotFound() {
        when(authValidationGateway.validateClientUser(testApplication.getIdDocument(), token)).thenReturn(Mono.just(user));
        when(findLoanTypeByIdUseCase.findById(testApplication.getIdLoanType()))
                .thenReturn(Mono.error(new EntityNotFoundException("Loan type not found")));

        StepVerifier.create(registerRequestUseCase.registerApplication(testApplication, token))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when status 'Pending Review' is not found")
    void registerApplicationStatusNotFound() {
        when(authValidationGateway.validateClientUser(testApplication.getIdDocument(), token)).thenReturn(Mono.just(user));
        when(findLoanTypeByIdUseCase.findById(testApplication.getIdLoanType())).thenReturn(Mono.just(loanType));
        when(applicationRepository.findActiveLoansByIdUser(user.getIdUser())).thenReturn(Flux.empty());
        when(statusRepository.findByName("Pending Review")).thenReturn(Mono.empty());

        StepVerifier.create(registerRequestUseCase.registerApplication(testApplication, token))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidAmountException when amount is null")
    void registerApplicationAmountNull() {
        testApplication.setAmount(null);
        when(transactionManager.executeInTransaction(any())).thenAnswer(invocation -> invocation.getArgument(0, Mono.class));
        when(authValidationGateway.validateClientUser(testApplication.getIdDocument(), token)).thenReturn(Mono.just(user));
        when(findLoanTypeByIdUseCase.findById(testApplication.getIdLoanType())).thenReturn(Mono.just(loanType));
        when(applicationRepository.findActiveLoansByIdUser(user.getIdUser())).thenReturn(Flux.empty());
        when(statusRepository.findByName("Pending Review")).thenReturn(Mono.just(status));

        StepVerifier.create(registerRequestUseCase.registerApplication(testApplication, token))
                .expectError(InvalidAmountException.class)
                .verify();

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidAmountException when amount is below minimum")
    void registerApplicationAmountBelowMin() {
        testApplication.setAmount(5000.0);
        when(transactionManager.executeInTransaction(any())).thenAnswer(invocation -> invocation.getArgument(0, Mono.class));
        when(authValidationGateway.validateClientUser(testApplication.getIdDocument(), token)).thenReturn(Mono.just(user));
        when(findLoanTypeByIdUseCase.findById(testApplication.getIdLoanType())).thenReturn(Mono.just(loanType));
        when(applicationRepository.findActiveLoansByIdUser(user.getIdUser())).thenReturn(Flux.empty());
        when(statusRepository.findByName("Pending Review")).thenReturn(Mono.just(status));

        StepVerifier.create(registerRequestUseCase.registerApplication(testApplication, token))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(InvalidAmountException.class);
                    assertThat(error.getMessage()).contains("outside the valid range");
                })
                .verify();

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidAmountException when amount is above maximum")
    void registerApplicationAmountAboveMax() {
        testApplication.setAmount(60000.0);
        when(transactionManager.executeInTransaction(any())).thenAnswer(invocation -> invocation.getArgument(0, Mono.class));
        when(authValidationGateway.validateClientUser(testApplication.getIdDocument(), token)).thenReturn(Mono.just(user));
        when(findLoanTypeByIdUseCase.findById(testApplication.getIdLoanType())).thenReturn(Mono.just(loanType));
        when(applicationRepository.findActiveLoansByIdUser(user.getIdUser())).thenReturn(Flux.empty());
        when(statusRepository.findByName("Pending Review")).thenReturn(Mono.just(status));

        StepVerifier.create(registerRequestUseCase.registerApplication(testApplication, token))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(InvalidAmountException.class);
                    assertThat(error.getMessage()).contains("outside the valid range");
                })
                .verify();

        verify(applicationRepository, never()).save(any());
    }
}
