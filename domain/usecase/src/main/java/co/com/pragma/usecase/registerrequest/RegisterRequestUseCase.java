package co.com.pragma.usecase.registerrequest;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.application.gateways.ApplicationRepository;
import co.com.pragma.model.auth.ValidatedUser;
import co.com.pragma.model.creditanalysis.CreditAnalysisPayload;
import co.com.pragma.model.creditanalysis.LoanDetails;
import co.com.pragma.model.creditanalysis.gateway.CreditAnalysisGateway;
import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.exception.InvalidAmountException;
import co.com.pragma.model.auth.gateway.AuthValidationGateway;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.gateways.TransactionManager;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.model.status.Status;
import co.com.pragma.model.status.gateways.StatusRepository;
import co.com.pragma.usecase.findloantypebyid.FindLoanTypeByIdUseCase;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;


@RequiredArgsConstructor
public class RegisterRequestUseCase {

    private final ApplicationRepository applicationRepository;
    private final StatusRepository statusRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final TransactionManager transactionManager;
    private final AuthValidationGateway authValidationGateway;
    private final FindLoanTypeByIdUseCase findLoanTypeByIdUseCase;
    private final CreditAnalysisGateway creditAnalysisGateway;
    private final CustomLogger customLogger;

    private static final String PENDING_REVIEW_STATUS_NAME = "Pending Review";
    private static final String APPROVED_STATUS_NAME = "Approved";

    public Mono<Application> registerApplication(Application application, String token) {
        customLogger.trace("Starting request registration for idDocument: {}", application.getIdDocument());

        return authValidationGateway.validateClientUser(application.getIdDocument(), token)
                .flatMap(user -> {
                    application.setEmail(user.getEmail().toLowerCase());
                    application.setIdUser(user.getIdUser());

                    return findLoanTypeByIdUseCase.findById(application.getIdLoanType())
                            .flatMap(newLoanType -> findActiveLoansAndTheirTypes(user.getIdUser())
                                    .flatMap(activeLoansData -> findPendingReviewStatus()
                                            .flatMap(pendingStatus -> {

                                                List<Application> activeLoans = activeLoansData.getT1();
                                                Map<UUID, LoanType> activeLoanTypesMap = activeLoansData.getT2();

                                                CreditAnalysisPayload payload = buildPayload(application, user, newLoanType, activeLoans, activeLoanTypesMap);
                                                return transactionManager.executeInTransaction(
                                                        validateAmount(application.getAmount(), newLoanType)
                                                                .then(Mono.fromCallable(() -> prepareApplication(application, pendingStatus)))
                                                                .flatMap(applicationRepository::save)
                                                                .flatMap(savedApp -> {
                                                                    payload.setIdApplication(savedApp.getId());
                                                                    payload.setIdUser(savedApp.getIdUser());
                                                                    if (Boolean.TRUE.equals(newLoanType.getAutomaticValidation())) {
                                                                        customLogger.trace("Enqueuing payload for application {}", savedApp.getId());
                                                                        return creditAnalysisGateway.requestAnalysis(payload).thenReturn(savedApp);
                                                                    }
                                                                    return Mono.just(savedApp);
                                                                })
                                                );
                                            })
                                    )
                            );
                })
                .doOnSuccess(savedApp -> customLogger.trace("Application registered successfully for email: {}", savedApp.getEmail()))
                .doOnError(error -> customLogger.trace("Application registration failed for {}: {}", application.getIdDocument(), error.getMessage()));
    }

    private Mono<Tuple2<List<Application>, Map<UUID, LoanType>>> findActiveLoansAndTheirTypes(UUID userId) {
        return applicationRepository.findActiveLoansByIdUser(userId).collectList()
                .flatMap(activeLoans -> {
                    if (activeLoans.isEmpty()) {
                        return Mono.just(Tuples.of(List.of(), Map.of()));
                    }

                    List<UUID> loanTypeIds = activeLoans.stream()
                            .map(Application::getIdLoanType)
                            .collect(Collectors.toList());

                    return loanTypeRepository.findByIds(loanTypeIds).collectList()
                            .map(loanTypes -> {
                                Map<UUID, LoanType> loanTypeMap = loanTypes.stream()
                                        .collect(Collectors.toMap(LoanType::getId, Function.identity()));
                                return Tuples.of(activeLoans, loanTypeMap);
                            });
                });
    }


    private CreditAnalysisPayload buildPayload(Application newApp, ValidatedUser user, LoanType newLoanType,
                                                  List<Application> activeLoans, Map<UUID, LoanType> activeLoanTypesMap) {
        LoanDetails newLoanDetails = LoanDetails.builder()
                .amount(newApp.getAmount())
                .term(newApp.getTerm())
                .interestRate(newLoanType.getInterestRate())
                .estado(PENDING_REVIEW_STATUS_NAME)
                .build();

        List<LoanDetails> activeLoansDetails = activeLoans.stream()
                .map(loan -> {
                    LoanType loanType = activeLoanTypesMap.get(loan.getIdLoanType());
                    return LoanDetails.builder()
                            .amount(loan.getAmount())
                            .term(loan.getTerm())
                            .interestRate(loanType.getInterestRate())
                            .estado(APPROVED_STATUS_NAME)
                            .build();
                })
                .collect(Collectors.toList());

        return CreditAnalysisPayload.builder()
                .idApplication(newApp.getId())
                .idUser(newApp.getIdUser())
                .idDocument(user.getIdDocument())
                .email(user.getEmail())
                .baseSalary(user.getBaseSalary())
                .newLoanDetails(newLoanDetails)
                .loanAssets(activeLoansDetails)
                .build();
    }

    private Mono<Void> validateAmount(Double amount, LoanType loanType) {
        return Mono.defer(() -> {
            if (amount == null) {
                customLogger.trace("Validation failed: amount is null for loan type {}", loanType.getName());
                return Mono.error(new InvalidAmountException("Amount cannot be null"));
            }
            if (amount < loanType.getMinAmount() || amount > loanType.getMaxAmount()) {
                customLogger.trace("Validation failed: amount {} is out of range [{}, {}] for loan type {}",
                        amount, loanType.getMinAmount(), loanType.getMaxAmount(), loanType.getName());
                return Mono.error(new InvalidAmountException(
                        String.format("Amount %.2f is outside the valid range [%.2f, %.2f] for loan type %s",
                                amount, loanType.getMinAmount(), loanType.getMaxAmount(), loanType.getName())
                ));
            }
            customLogger.trace("Amount {} is valid for loan type {}", amount, loanType.getName());
            return Mono.empty();
        });
    }

    private Mono<Status> findPendingReviewStatus() {
        return statusRepository.findByName(PENDING_REVIEW_STATUS_NAME)
                .switchIfEmpty(Mono.defer(() -> {
                    customLogger.trace("Status 'Pending Review' not found");
                    return Mono.error(new EntityNotFoundException("Status 'Pending Review' not found"));
                }))
                .doOnSuccess(status ->
                        customLogger.trace("Status 'Pending Review' found"));
    }

    private Application prepareApplication(Application application, Status status) {
        customLogger.trace("Preparing application for saving with status 'Pending Review'");
        application.setIdStatus(status.getId());
        return application;
    }
}
