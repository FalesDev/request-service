package co.com.pragma.usecase.registerrequest;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.application.gateways.ApplicationRepository;
import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.exception.InvalidAmountException;
import co.com.pragma.model.gateways.TransactionalGateway;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.model.status.gateways.StatusRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RegisterRequestUseCase {

    private final ApplicationRepository applicationRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final StatusRepository statusRepository;
    private final TransactionalGateway transactionalGateway;

    public Mono<Application> registerApplication(Application application) {
        return transactionalGateway.executeInTransaction(
                loanTypeRepository.findById(application.getIdLoanType())
                        .switchIfEmpty(Mono.error(new EntityNotFoundException("LoanType not found")))
                        .flatMap(loanType -> validateAmount(application.getAmount(), loanType)
                                .then(Mono.just(loanType)))
                        .flatMap(loanType -> statusRepository.findByName("Pending Review")
                                .switchIfEmpty(Mono.error(new EntityNotFoundException("Status 'Pending Review' not found")))
                                .flatMap(status -> {
                                    application.setIdStatus(status.getId());
                                    return applicationRepository.save(application);
                                })
                        )
        );
    }

    private Mono<Void> validateAmount(Double amount, LoanType loanType) {
        return Mono.defer(() -> {
            if (amount == null) {
                return Mono.error(new InvalidAmountException("Amount cannot be null"));
            }
            if (amount < loanType.getMinAmount() || amount > loanType.getMaxAmount()) {
                return Mono.error(new InvalidAmountException(
                        String.format("Amount %.2f is outside the valid range [%.2f, %.2f] for loan type %s",
                                amount, loanType.getMinAmount(), loanType.getMaxAmount(), loanType.getName())
                ));
            }
            return Mono.empty();
        });
    }
}
