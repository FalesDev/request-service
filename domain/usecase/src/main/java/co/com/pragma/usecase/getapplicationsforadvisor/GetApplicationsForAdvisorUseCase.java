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
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@RequiredArgsConstructor
public class GetApplicationsForAdvisorUseCase {

    private final ApplicationRepository applicationRepository;
    private final StatusRepository statusRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final AuthValidationGateway authValidationGateway;
    private final CustomLogger logger;

    private static final String APPROVED_STATUS_NAME = "Approved";

    public Mono<CustomPage<ApplicationAdvisorView>> getApplicationsByStatus(
            String token,
            List<String> statusNames,
            CustomPageable pageable) {

        logger.trace("Starting search for applications for advisor with statuses: {}", statusNames);

        return statusRepository.findByNames(statusNames)
                .collectList()
                .flatMap(statuses -> {
                    if (statuses.isEmpty()) {
                        logger.trace("No states found: {}", statusNames);
                        return Mono.just(createEmptyPage());
                    }
                    List<UUID> statusIds = statuses.stream().map(Status::getId).toList();
                    logger.trace("State IDs found: {}", statusIds);
                    return applicationRepository.findByIdStatusIn(statusIds, pageable)
                            .flatMap(applicationPage ->
                                    convertToAdvisorViewPage(applicationPage, token)
                            );
                });
    }

    private Mono<CustomPage<ApplicationAdvisorView>> convertToAdvisorViewPage(
            CustomPage<Application> applicationPage, String token) {

        List<Application> applications = applicationPage.getContent();
        if (applications.isEmpty()) {
            return Mono.just(createEmptyPageFrom(applicationPage));
        }

        List<UUID> userIds = applicationPage.getContent().stream()
                .map(Application::getIdUser)
                .distinct()
                .toList();

        Mono<Map<UUID, UserFound>> usersMapMono = authValidationGateway.foundClientByIds(userIds, token)
                .collectMap(UserFound::getIdUser, Function.identity());

        return usersMapMono.flatMap(usersMap -> {
            return Flux.fromIterable(applications)
                    .flatMap(application -> {
                        UserFound clientUser = usersMap.get(application.getIdUser());
                        if (clientUser == null) {
                            logger.warn("User data not found for userId: {}. Skipping application.", application.getIdUser());
                            return Mono.empty();
                        }
                        return buildApplicationView(application, clientUser);
                    })
                    .collectList()
                    .map(advisorViews ->
                            CustomPage.<ApplicationAdvisorView>builder()
                                    .content(advisorViews)
                                    .currentPage(applicationPage.getCurrentPage())
                                    .totalPages(applicationPage.getTotalPages())
                                    .totalElements(applicationPage.getTotalElements())
                                    .pageSize(applicationPage.getPageSize())
                                    .hasNext(applicationPage.isHasNext())
                                    .hasPrevious(applicationPage.isHasPrevious())
                                    .build()
                    );
        });
    }

    private Mono<ApplicationAdvisorView> buildApplicationView(Application application, UserFound clientUser) {
        return Mono.zip(
                loanTypeRepository.findById(application.getIdLoanType()),
                statusRepository.findById(application.getIdStatus()),
                calculateTotalMonthlyDebtForUser(application.getIdUser())
        ).map(tuple -> {
            LoanType loanType = tuple.getT1();
            Status status = tuple.getT2();
            BigDecimal totalMonthlyDebt = tuple.getT3();

            return ApplicationAdvisorView.builder()
                    .amount(application.getAmount())
                    .term(application.getTerm())
                    .email(clientUser.getEmail())
                    .fullName(clientUser.getFirstName() + " " + clientUser.getLastName())
                    .loanTypeName(loanType.getName())
                    .interestRate(loanType.getInterestRate())
                    .statusName(status.getName())
                    .baseSalary(clientUser.getBaseSalary())
                    .totalMonthlyDebt(totalMonthlyDebt)
                    .build();
        });
    }

    private CustomPage<ApplicationAdvisorView> createEmptyPage() {
        return CustomPage.<ApplicationAdvisorView>builder().content(Collections.emptyList()).build();
    }

    private <T> CustomPage<ApplicationAdvisorView> createEmptyPageFrom(CustomPage<T> sourcePage) {
        return CustomPage.<ApplicationAdvisorView>builder()
                .content(Collections.emptyList())
                .currentPage(sourcePage.getCurrentPage())
                .totalPages(sourcePage.getTotalPages())
                .totalElements(sourcePage.getTotalElements())
                .pageSize(sourcePage.getPageSize())
                .hasNext(sourcePage.isHasNext())
                .hasPrevious(sourcePage.isHasPrevious())
                .build();
    }

    /**
     * Calcula la deuda mensual total sumando las cuotas de todas las solicitudes aprobadas
     * para un usuario específico.
     */
    private Mono<BigDecimal> calculateTotalMonthlyDebtForUser(UUID idUser) {
        logger.trace("Calculate total monthly debt for the user: {}", idUser);
        return statusRepository.findByName(APPROVED_STATUS_NAME)
                .map(Status::getId)
                .flatMapMany(approvedStatusId ->
                        applicationRepository.findByIdUserAndIdStatus(idUser, approvedStatusId)
                )
                .flatMap(this::calculateApplicationMonthlyPayment)
                .reduce(0.0, Double::sum)
                .defaultIfEmpty(0.0)
                .map(total -> BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP))
                .doOnSuccess(total -> logger.trace("Total monthly debt for user {} is: {}", idUser, total));
    }

    /**
     * Calcula la cuota mensual para una única solicitud obteniendo su tipo de préstamo.
     */
    private Mono<Double> calculateApplicationMonthlyPayment(Application application) {
        return loanTypeRepository.findById(application.getIdLoanType())
                .map(loanType -> calculateMonthlyPayment(
                        application.getAmount(),
                        loanType.getInterestRate(),
                        application.getTerm()
                ));
    }

    /**
     * Fórmula de amortización para calcular la cuota mensual de un préstamo.
     * M = P * [r(1+r)^n] / [(1+r)^n - 1]
     */
    private double calculateMonthlyPayment(double principal, double annualInterestRate, int termInMonths) {
        if (annualInterestRate <= 0 || termInMonths <= 0) {
            return principal / (termInMonths > 0 ? termInMonths : 1);
        }
        double monthlyRate = (annualInterestRate / 100) / 12;
        double ratePower = Math.pow(1 + monthlyRate, termInMonths);
        return principal * (monthlyRate * ratePower) / (ratePower - 1);
    }
}