package co.com.pragma.usecase.registerrequest;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.application.gateways.ApplicationRepository;
import co.com.pragma.model.auth.ValidatedUser;
import co.com.pragma.model.auth.gateway.AuthValidationGateway;
import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.exception.InvalidAmountException;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.gateways.TransactionManager;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegisterRequestUseCaseUnitTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private LoanTypeRepository loanTypeRepository;
    @Mock
    private StatusRepository statusRepository;
    @Mock
    private TransactionManager transactionManager;
    @Mock
    private AuthValidationGateway authValidationGateway;
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
        user = ValidatedUser.builder()
                .idUser(UUID.randomUUID())
                .email("FABRICIO@GMAIL.COM")
                .build();

        testApplication = Application.builder()
                .id(UUID.randomUUID())
                .amount(20000.0)
                .term(12)
                .idDocument("77777777")
                .idLoanType(UUID.randomUUID())
                .build();

        loanType = LoanType.builder()
                .id(UUID.randomUUID())
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

        when(transactionManager.executeInTransaction(any())).thenAnswer(invocation ->
                invocation.getArgument(0, Mono.class));
    }

    @Test
    @DisplayName("Should register application successfully when loan type and status exist and amount is valid")
    void registerApplicationSuccess() {
        when(authValidationGateway.validateClientUser(testApplication.getIdDocument(), token))
                .thenReturn(Mono.just(user));
        when(loanTypeRepository.findById(testApplication.getIdLoanType())).thenReturn(Mono.just(loanType));
        when(statusRepository.findByName("Pending Review")).thenReturn(Mono.just(status));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(registerRequestUseCase.registerApplication(testApplication, token))
                .expectNextMatches(app ->
                        app.getEmail().equals("fabricio@gmail.com") &&
                                app.getIdStatus().equals(status.getId())
                )
                .verifyComplete();

        verify(authValidationGateway).validateClientUser(testApplication.getIdDocument(), token);
        verify(loanTypeRepository).findById(testApplication.getIdLoanType());
        verify(statusRepository).findByName("Pending Review");
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when loan type is not found")
    void registerApplicationLoanTypeNotFound() {
        when(authValidationGateway.validateClientUser(testApplication.getIdDocument(), token))
                .thenReturn(Mono.just(user));
        when(loanTypeRepository.findById(testApplication.getIdLoanType())).thenReturn(Mono.empty());
        when(statusRepository.findByName("Pending Review")).thenReturn(Mono.just(status));

        StepVerifier.create(registerRequestUseCase.registerApplication(testApplication, token))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(authValidationGateway).validateClientUser(testApplication.getIdDocument(), token);
        verify(loanTypeRepository).findById(testApplication.getIdLoanType());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidAmountException when amount is null")
    void registerApplicationAmountNull() {
        testApplication.setAmount(null);
        when(authValidationGateway.validateClientUser(testApplication.getIdDocument(), token))
                .thenReturn(Mono.just(user));
        when(loanTypeRepository.findById(testApplication.getIdLoanType())).thenReturn(Mono.just(loanType));
        when(statusRepository.findByName("Pending Review")).thenReturn(Mono.just(status));

        StepVerifier.create(registerRequestUseCase.registerApplication(testApplication, token))
                .expectError(InvalidAmountException.class)
                .verify();

        verify(authValidationGateway).validateClientUser(testApplication.getIdDocument(), token);
        verify(loanTypeRepository).findById(testApplication.getIdLoanType());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when status 'Pending Review' is not found")
    void registerApplicationStatusNotFound() {
        when(authValidationGateway.validateClientUser(testApplication.getIdDocument(), token))
                .thenReturn(Mono.just(user));
        when(loanTypeRepository.findById(testApplication.getIdLoanType())).thenReturn(Mono.just(loanType));
        when(statusRepository.findByName("Pending Review")).thenReturn(Mono.empty());

        StepVerifier.create(registerRequestUseCase.registerApplication(testApplication, token))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(authValidationGateway).validateClientUser(testApplication.getIdDocument(), token);
        verify(loanTypeRepository).findById(testApplication.getIdLoanType());
        verify(statusRepository).findByName("Pending Review");
        verify(applicationRepository, never()).save(any());
    }
}
