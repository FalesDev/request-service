package co.com.pragma.usecase.getapplicationsforadvisor;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.application.gateways.ApplicationRepository;
import co.com.pragma.model.auth.UserFound;
import co.com.pragma.model.auth.gateway.AuthValidationGateway;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.model.pagination.CustomPage;
import co.com.pragma.model.pagination.CustomPageable;
import co.com.pragma.model.status.Status;
import co.com.pragma.model.status.gateways.StatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetApplicationsForAdvisorUseCaseTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock
    private StatusRepository statusRepository;
    @Mock private LoanTypeRepository loanTypeRepository;
    @Mock private AuthValidationGateway authValidationGateway;
    @Mock private CustomLogger logger;

    @InjectMocks
    private GetApplicationsForAdvisorUseCase useCase;

    private Status status;
    private Application application;
    private LoanType loanType;
    private UserFound user;
    private CustomPageable pageable;

    @BeforeEach
    void setUp() {
        status = Status.builder()
                .id(UUID.randomUUID())
                .name("Pending Review")
                .build();

        application = Application.builder()
                .id(UUID.randomUUID())
                .idUser(UUID.randomUUID())
                .idLoanType(UUID.randomUUID())
                .idStatus(status.getId())
                .amount(10000.0)
                .term(12)
                .build();

        loanType = LoanType.builder()
                .id(application.getIdLoanType())
                .name("Car Loan")
                .interestRate(12.0)
                .build();

        user = UserFound.builder()
                .idUser(application.getIdUser())
                .email("test@test.com")
                .firstName("John")
                .lastName("Doe")
                .baseSalary(5000.0)
                .build();

        pageable = CustomPageable.builder().page(0).size(10).build();
    }

    @Test
    @DisplayName("Should return empty page when no statuses are found")
    void shouldReturnEmptyPageWhenNoStatusesFound() {
        when(statusRepository.findByNames(List.of("Nonexistent"))).thenReturn(Flux.empty());

        StepVerifier.create(useCase.getApplicationsByStatus("token", List.of("Nonexistent"), pageable))
                .assertNext(page -> {
                    assertThat(page.getContent()).isEmpty();
                })
                .verifyComplete();

        verify(applicationRepository, never()).findByIdStatusIn(anyList(), any());
    }

    @Test
    @DisplayName("Should return applications mapped to advisor view when statuses exist")
    void shouldReturnApplicationsMappedToAdvisorView() {
        when(statusRepository.findByNames(List.of("Pending Review")))
                .thenReturn(Flux.just(status));
        when(applicationRepository.findByIdStatusIn(List.of(status.getId()), pageable))
                .thenReturn(Mono.just(CustomPage.<Application>builder()
                        .content(List.of(application))
                        .currentPage(0)
                        .totalPages(1)
                        .totalElements(1)
                        .pageSize(10)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build()));
        when(authValidationGateway.foundClientUserById(application.getIdUser(), "token"))
                .thenReturn(Mono.just(user));
        when(loanTypeRepository.findById(application.getIdLoanType()))
                .thenReturn(Mono.just(loanType));
        when(statusRepository.findById(application.getIdStatus()))
                .thenReturn(Mono.just(status));
        when(statusRepository.findByName("Approved"))
                .thenReturn(Mono.just(Status.builder().id(UUID.randomUUID()).name("Approved").build()));
        when(applicationRepository.findByIdUserAndIdStatus(eq(application.getIdUser()), any()))
                .thenReturn(Flux.empty());

        StepVerifier.create(useCase.getApplicationsByStatus("token", List.of("Pending Review"), pageable))
                .assertNext(page -> {
                    assertThat(page.getContent()).hasSize(1);
                    var view = page.getContent().getFirst();
                    assertThat(view.getEmail()).isEqualTo("test@test.com");
                    assertThat(view.getFullName()).isEqualTo("John Doe");
                    assertThat(view.getLoanTypeName()).isEqualTo("Car Loan");
                    assertThat(view.getStatusName()).isEqualTo("Pending Review");
                    assertThat(view.getTotalMonthlyDebt()).isEqualByComparingTo("0.00");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should calculate monthly debt for user with approved applications")
    void shouldCalculateMonthlyDebtForApprovedApplications() {
        var approvedStatus = Status.builder().id(UUID.randomUUID()).name("Approved").build();

        when(statusRepository.findByNames(anyList())).thenReturn(Flux.just(status));
        when(applicationRepository.findByIdStatusIn(anyList(), eq(pageable)))
                .thenReturn(Mono.just(CustomPage.<Application>builder()
                        .content(List.of(application))
                        .build()));
        when(authValidationGateway.foundClientUserById(any(), any()))
                .thenReturn(Mono.just(user));
        when(loanTypeRepository.findById(application.getIdLoanType()))
                .thenReturn(Mono.just(loanType));
        when(statusRepository.findById(application.getIdStatus())).thenReturn(Mono.just(status));
        when(statusRepository.findByName("Approved")).thenReturn(Mono.just(approvedStatus));
        when(applicationRepository.findByIdUserAndIdStatus(application.getIdUser(), approvedStatus.getId()))
                .thenReturn(Flux.just(application));

        StepVerifier.create(useCase.getApplicationsByStatus("token", List.of("Pending Review"), pageable))
                .assertNext(page -> {
                    var debt = page.getContent().get(0).getTotalMonthlyDebt();
                    assertThat(debt).isGreaterThan(BigDecimal.ZERO);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should propagate error when repository fails")
    void shouldPropagateErrorWhenRepositoryFails() {
        when(statusRepository.findByNames(anyList()))
                .thenReturn(Flux.error(new RuntimeException("DB error")));

        StepVerifier.create(useCase.getApplicationsByStatus("token", List.of("Any"), pageable))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("DB error"))
                .verify();
    }

    @Test
    @DisplayName("Should calculate monthly payment when interest rate is zero")
    void shouldCalculateMonthlyPaymentWithZeroInterest() {
        var loanTypeZeroRate = LoanType.builder()
                .id(application.getIdLoanType())
                .name("Zero Interest Loan")
                .interestRate(0.0) // <= 0
                .build();

        when(statusRepository.findByNames(anyList())).thenReturn(Flux.just(status));
        when(applicationRepository.findByIdStatusIn(anyList(), eq(pageable)))
                .thenReturn(Mono.just(CustomPage.<Application>builder()
                        .content(List.of(application))
                        .build()));
        when(authValidationGateway.foundClientUserById(any(), any())).thenReturn(Mono.just(user));
        when(loanTypeRepository.findById(application.getIdLoanType()))
                .thenReturn(Mono.just(loanTypeZeroRate)); // fuerza tasa = 0
        when(statusRepository.findById(application.getIdStatus())).thenReturn(Mono.just(status));
        when(statusRepository.findByName("Approved"))
                .thenReturn(Mono.just(Status.builder().id(UUID.randomUUID()).name("Approved").build()));
        when(applicationRepository.findByIdUserAndIdStatus(eq(application.getIdUser()), any()))
                .thenReturn(Flux.just(application));

        StepVerifier.create(useCase.getApplicationsByStatus("token", List.of("Pending Review"), pageable))
                .assertNext(page -> {
                    var payment = page.getContent().getFirst().getTotalMonthlyDebt();
                    assertThat(payment).isGreaterThanOrEqualTo(BigDecimal.ZERO);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should calculate monthly payment when term is zero")
    void shouldCalculateMonthlyPaymentWithZeroTerm() {
        var applicationZeroTerm = application.toBuilder().term(0).build();

        when(statusRepository.findByNames(anyList())).thenReturn(Flux.just(status));
        when(applicationRepository.findByIdStatusIn(anyList(), eq(pageable)))
                .thenReturn(Mono.just(CustomPage.<Application>builder()
                        .content(List.of(applicationZeroTerm))
                        .build()));
        when(authValidationGateway.foundClientUserById(any(), any())).thenReturn(Mono.just(user));
        when(loanTypeRepository.findById(applicationZeroTerm.getIdLoanType()))
                .thenReturn(Mono.just(loanType));
        when(statusRepository.findById(applicationZeroTerm.getIdStatus())).thenReturn(Mono.just(status));
        when(statusRepository.findByName("Approved"))
                .thenReturn(Mono.just(Status.builder().id(UUID.randomUUID()).name("Approved").build()));
        when(applicationRepository.findByIdUserAndIdStatus(eq(applicationZeroTerm.getIdUser()), any()))
                .thenReturn(Flux.just(applicationZeroTerm));

        StepVerifier.create(useCase.getApplicationsByStatus("token", List.of("Pending Review"), pageable))
                .assertNext(page -> {
                    var payment = page.getContent().getFirst().getTotalMonthlyDebt();
                    assertThat(payment).isGreaterThanOrEqualTo(BigDecimal.ZERO);
                })
                .verifyComplete();
    }

}
