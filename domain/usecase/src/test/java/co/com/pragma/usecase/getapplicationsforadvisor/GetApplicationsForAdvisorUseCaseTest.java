package co.com.pragma.usecase.getapplicationsforadvisor;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.application.ApplicationAdvisorView;
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
    private Status approvedStatus;

    @BeforeEach
    void setUp() {
        status = Status.builder()
                .id(UUID.randomUUID())
                .name("Pending Review")
                .build();

        approvedStatus = Status.builder()
                .id(UUID.randomUUID())
                .name("Approved")
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
        when(statusRepository.findByNames(List.of("Pending Review"))).thenReturn(Flux.just(status));
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
        when(authValidationGateway.foundClientByIds(List.of(application.getIdUser()), "token"))
                .thenReturn(Flux.just(user));

        when(loanTypeRepository.findById(application.getIdLoanType())).thenReturn(Mono.just(loanType));
        when(statusRepository.findById(application.getIdStatus())).thenReturn(Mono.just(status));

        when(statusRepository.findByName("Approved"))
                .thenReturn(Mono.just(Status.builder().id(UUID.randomUUID()).name("Approved").build()));
        when(applicationRepository.findByIdUserAndIdStatus(eq(application.getIdUser()), any()))
                .thenReturn(Flux.empty());

        StepVerifier.create(useCase.getApplicationsByStatus("token", List.of("Pending Review"), pageable))
                .assertNext(page -> {
                    assertThat(page.getContent()).hasSize(1);
                    var view = page.getContent().get(0);
                    assertThat(view.getEmail()).isEqualTo(user.getEmail());
                    assertThat(view.getFullName()).isEqualTo(user.getFirstName() + " " + user.getLastName());
                    assertThat(view.getLoanTypeName()).isEqualTo(loanType.getName());
                    assertThat(view.getStatusName()).isEqualTo(status.getName());
                    assertThat(view.getTotalMonthlyDebt()).isEqualByComparingTo("0.00");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should calculate monthly debt for user with approved applications")
    void shouldCalculateMonthlyDebtForApprovedApplications() {
        Status approvedStatus = Status.builder().id(UUID.randomUUID()).name("Approved").build();

        when(statusRepository.findByNames(anyList())).thenReturn(Flux.just(status));
        when(applicationRepository.findByIdStatusIn(anyList(), eq(pageable)))
                .thenReturn(Mono.just(CustomPage.<Application>builder().content(List.of(application)).build()));
        when(authValidationGateway.foundClientByIds(anyList(), any())).thenReturn(Flux.just(user));
        when(loanTypeRepository.findById(application.getIdLoanType())).thenReturn(Mono.just(loanType));
        when(statusRepository.findById(application.getIdStatus())).thenReturn(Mono.just(status));
        when(statusRepository.findByName("Approved")).thenReturn(Mono.just(approvedStatus));
        when(applicationRepository.findByIdUserAndIdStatus(application.getIdUser(), approvedStatus.getId()))
                .thenReturn(Flux.just(application));

        StepVerifier.create(useCase.getApplicationsByStatus("token", List.of("Pending Review"), pageable))
                .assertNext(page -> assertThat(page.getContent().get(0).getTotalMonthlyDebt()).isGreaterThan(BigDecimal.ZERO))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should ignore application if user not found")
    void shouldIgnoreApplicationIfUserNotFound() {
        when(statusRepository.findByNames(anyList())).thenReturn(Flux.just(status));
        when(applicationRepository.findByIdStatusIn(anyList(), eq(pageable)))
                .thenReturn(Mono.just(CustomPage.<Application>builder().content(List.of(application)).build()));
        when(authValidationGateway.foundClientByIds(anyList(), any())).thenReturn(Flux.empty());

        StepVerifier.create(useCase.getApplicationsByStatus("token", List.of("Pending Review"), pageable))
                .assertNext(page -> assertThat(page.getContent()).isEmpty())
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
        LoanType zeroInterestLoanType = LoanType.builder()
                .id(application.getIdLoanType())
                .name("Zero Interest Loan")
                .interestRate(0.0)
                .build();

        CustomPage<Application> applicationPage = CustomPage.<Application>builder()
                .content(List.of(application))
                .currentPage(0)
                .totalPages(1)
                .totalElements(1)
                .pageSize(10)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(statusRepository.findByNames(List.of("Pending Review")))
                .thenReturn(Flux.just(status));
        when(applicationRepository.findByIdStatusIn(List.of(status.getId()), pageable))
                .thenReturn(Mono.just(applicationPage));
        when(authValidationGateway.foundClientByIds(List.of(application.getIdUser()), "token"))
                .thenReturn(Flux.just(user));
        when(loanTypeRepository.findById(application.getIdLoanType()))
                .thenReturn(Mono.just(zeroInterestLoanType));
        when(statusRepository.findById(application.getIdStatus()))
                .thenReturn(Mono.just(status));
        when(statusRepository.findByName("Approved"))
                .thenReturn(Mono.just(approvedStatus));
        when(applicationRepository.findByIdUserAndIdStatus(eq(application.getIdUser()), eq(approvedStatus.getId())))
                .thenReturn(Flux.just(application));
        when(loanTypeRepository.findById(application.getIdLoanType()))
                .thenReturn(Mono.just(zeroInterestLoanType));

        StepVerifier.create(useCase.getApplicationsByStatus("token", List.of("Pending Review"), pageable))
                .assertNext(page -> {
                    ApplicationAdvisorView view = page.getContent().getFirst();
                    BigDecimal expectedPayment = BigDecimal.valueOf(10000.0 / 12).setScale(2, BigDecimal.ROUND_HALF_UP);
                    assertThat(view.getTotalMonthlyDebt()).isEqualByComparingTo(expectedPayment);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle empty approved applications when calculating monthly debt")
    void shouldHandleEmptyApprovedApplications() {
        CustomPage<Application> applicationPage = CustomPage.<Application>builder()
                .content(List.of(application))
                .currentPage(0)
                .totalPages(1)
                .totalElements(1)
                .pageSize(10)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(statusRepository.findByNames(List.of("Pending Review")))
                .thenReturn(Flux.just(status));
        when(applicationRepository.findByIdStatusIn(List.of(status.getId()), pageable))
                .thenReturn(Mono.just(applicationPage));
        when(authValidationGateway.foundClientByIds(List.of(application.getIdUser()), "token"))
                .thenReturn(Flux.just(user));
        when(loanTypeRepository.findById(application.getIdLoanType()))
                .thenReturn(Mono.just(loanType));
        when(statusRepository.findById(application.getIdStatus()))
                .thenReturn(Mono.just(status));
        when(statusRepository.findByName("Approved"))
                .thenReturn(Mono.just(approvedStatus));
        when(applicationRepository.findByIdUserAndIdStatus(eq(application.getIdUser()), eq(approvedStatus.getId())))
                .thenReturn(Flux.empty()); // No approved applications

        StepVerifier.create(useCase.getApplicationsByStatus("token", List.of("Pending Review"), pageable))
                .assertNext(page -> {
                    ApplicationAdvisorView view = page.getContent().get(0);
                    assertThat(view.getTotalMonthlyDebt()).isEqualByComparingTo(BigDecimal.ZERO);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle multiple users in the same page")
    void shouldHandleMultipleUsersInSamePage() {
        UUID secondUserId = UUID.randomUUID();
        Application secondApplication = Application.builder()
                .id(UUID.randomUUID())
                .idUser(secondUserId)
                .idLoanType(UUID.randomUUID())
                .idStatus(status.getId())
                .amount(15000.0)
                .term(24)
                .build();

        UserFound secondUser = UserFound.builder()
                .idUser(secondUserId)
                .email("second@test.com")
                .firstName("Jane")
                .lastName("Smith")
                .baseSalary(6000.0)
                .build();

        CustomPage<Application> applicationPage = CustomPage.<Application>builder()
                .content(List.of(application, secondApplication))
                .currentPage(0)
                .totalPages(1)
                .totalElements(2)
                .pageSize(10)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(statusRepository.findByNames(List.of("Pending Review")))
                .thenReturn(Flux.just(status));
        when(applicationRepository.findByIdStatusIn(List.of(status.getId()), pageable))
                .thenReturn(Mono.just(applicationPage));
        when(authValidationGateway.foundClientByIds(List.of(application.getIdUser(), secondUserId), "token"))
                .thenReturn(Flux.just(user, secondUser));
        when(loanTypeRepository.findById(application.getIdLoanType()))
                .thenReturn(Mono.just(loanType));
        when(loanTypeRepository.findById(secondApplication.getIdLoanType()))
                .thenReturn(Mono.just(loanType));
        when(statusRepository.findById(application.getIdStatus()))
                .thenReturn(Mono.just(status));
        when(statusRepository.findById(secondApplication.getIdStatus()))
                .thenReturn(Mono.just(status));
        when(statusRepository.findByName("Approved"))
                .thenReturn(Mono.just(approvedStatus));
        when(applicationRepository.findByIdUserAndIdStatus(eq(application.getIdUser()), eq(approvedStatus.getId())))
                .thenReturn(Flux.empty());
        when(applicationRepository.findByIdUserAndIdStatus(eq(secondUserId), eq(approvedStatus.getId())))
                .thenReturn(Flux.empty());

        StepVerifier.create(useCase.getApplicationsByStatus("token", List.of("Pending Review"), pageable))
                .assertNext(page -> {
                    assertThat(page.getContent()).hasSize(2);
                    assertThat(page.getContent().get(0).getEmail()).isEqualTo("test@test.com");
                    assertThat(page.getContent().get(1).getEmail()).isEqualTo("second@test.com");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should create empty page from existing CustomPage metadata")
    void shouldCreateEmptyPageFromCustomPage() {
        CustomPage<Application> emptyApplicationPage = CustomPage.<Application>builder()
                .content(List.of())
                .currentPage(1)
                .totalPages(5)
                .totalElements(0)
                .pageSize(10)
                .hasNext(true)
                .hasPrevious(false)
                .build();

        when(statusRepository.findByNames(List.of("Pending Review")))
                .thenReturn(Flux.just(status));
        when(applicationRepository.findByIdStatusIn(List.of(status.getId()), pageable))
                .thenReturn(Mono.just(emptyApplicationPage));

        StepVerifier.create(useCase.getApplicationsByStatus("token", List.of("Pending Review"), pageable))
                .assertNext(page -> {
                    assertThat(page.getContent()).isEmpty();
                    assertThat(page.getCurrentPage()).isEqualTo(1);
                    assertThat(page.getTotalPages()).isEqualTo(5);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should calculate monthly payment with interest rate greater than zero")
    void shouldCalculateMonthlyPaymentWithInterest() {
        LoanType loanWithInterest = LoanType.builder()
                .id(application.getIdLoanType())
                .name("Interest Loan")
                .interestRate(12.0)
                .build();

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
        when(authValidationGateway.foundClientByIds(List.of(application.getIdUser()), "token"))
                .thenReturn(Flux.just(user));
        when(loanTypeRepository.findById(application.getIdLoanType())).thenReturn(Mono.just(loanWithInterest));
        when(statusRepository.findById(application.getIdStatus())).thenReturn(Mono.just(status));
        when(statusRepository.findByName("Approved")).thenReturn(Mono.just(approvedStatus));
        when(applicationRepository.findByIdUserAndIdStatus(eq(application.getIdUser()), eq(approvedStatus.getId())))
                .thenReturn(Flux.just(application));

        StepVerifier.create(useCase.getApplicationsByStatus("token", List.of("Pending Review"), pageable))
                .assertNext(page -> {
                    ApplicationAdvisorView view = page.getContent().get(0);
                    assertThat(view.getTotalMonthlyDebt()).isGreaterThan(BigDecimal.ZERO);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should correctly map pagination info in convertToAdvisorViewPage")
    void shouldMapPaginationInfo() {
        CustomPage<Application> applicationPage = CustomPage.<Application>builder()
                .content(List.of(application))
                .currentPage(2)
                .totalPages(5)
                .totalElements(50)
                .pageSize(10)
                .hasNext(true)
                .hasPrevious(true)
                .build();

        when(statusRepository.findByNames(List.of("Pending Review"))).thenReturn(Flux.just(status));
        when(applicationRepository.findByIdStatusIn(List.of(status.getId()), pageable))
                .thenReturn(Mono.just(applicationPage));
        when(authValidationGateway.foundClientByIds(List.of(application.getIdUser()), "token"))
                .thenReturn(Flux.just(user));
        when(loanTypeRepository.findById(application.getIdLoanType())).thenReturn(Mono.just(loanType));
        when(statusRepository.findById(application.getIdStatus())).thenReturn(Mono.just(status));
        when(statusRepository.findByName("Approved")).thenReturn(Mono.just(approvedStatus));
        when(applicationRepository.findByIdUserAndIdStatus(eq(application.getIdUser()), eq(approvedStatus.getId())))
                .thenReturn(Flux.empty());

        StepVerifier.create(useCase.getApplicationsByStatus("token", List.of("Pending Review"), pageable))
                .assertNext(page -> {
                    assertThat(page.getCurrentPage()).isEqualTo(2);
                    assertThat(page.getTotalPages()).isEqualTo(5);
                    assertThat(page.isHasNext()).isTrue();
                    assertThat(page.isHasPrevious()).isTrue();
                })
                .verifyComplete();
    }
}
