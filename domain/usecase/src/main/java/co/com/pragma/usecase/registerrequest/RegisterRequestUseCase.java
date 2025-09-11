package co.com.pragma.usecase.registerrequest;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.application.gateways.ApplicationRepository;
import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.exception.InvalidAmountException;
import co.com.pragma.model.auth.gateway.AuthValidationGateway;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.gateways.TransactionManager;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.status.Status;
import co.com.pragma.model.status.gateways.StatusRepository;
import co.com.pragma.usecase.findloantypebyid.FindLoanTypeByIdUseCase;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;


@RequiredArgsConstructor
public class RegisterRequestUseCase {

    private final ApplicationRepository applicationRepository;
    private final StatusRepository statusRepository;
    private final TransactionManager transactionManager;
    private final AuthValidationGateway authValidationGateway;
    private final FindLoanTypeByIdUseCase findLoanTypeByIdUseCase;
    private final CustomLogger customLogger;

    public Mono<Application> registerApplication(Application application, String token) {
        customLogger.trace("Starting request registration for idDocument: {}", application.getIdDocument());

        return authValidationGateway.validateClientUser(application.getIdDocument(), token)
                .flatMap(user -> {
                    application.setEmail(user.getEmail().toLowerCase());
                    application.setIdUser(user.getIdUser());
                    return transactionManager.executeInTransaction(
                            findLoanTypeByIdUseCase.findById(application.getIdLoanType())
                                    .flatMap(loanType ->
                                            validateAmount(application.getAmount(), loanType)
                                                    .then(Mono.just(loanType))
                                    )
                                    .flatMap(loanType ->
                                            findPendingReviewStatus()
                                                    .map(status -> prepareApplication(application, status))
                                    )
                                    .flatMap(applicationRepository::save)
                                    .doOnSuccess(savedApp ->
                                            customLogger.trace("Application registered successfully for email: {}", savedApp.getEmail()))
                                    .doOnError(error ->
                                            customLogger.trace("Application registration failed for {}: {}", application.getIdDocument(), error.getMessage()))
                    );
                });
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
        return statusRepository.findByName("Pending Review")
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
