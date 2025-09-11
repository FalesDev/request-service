package co.com.pragma.usecase.findloantypebyid;

import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class FindLoanTypeByIdUseCase {

    private final LoanTypeRepository loanTypeRepository;
    private final CustomLogger customLogger;

    public Mono<LoanType> findById(UUID idLoanType) {
        customLogger.trace("Finding LoanType by id: {}", idLoanType);
        return loanTypeRepository.findById(idLoanType)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("LoanType not found")))
                .doOnSuccess(loanType -> customLogger.trace("LoanType found successfully with id: {}", idLoanType))
                .doOnError(error -> customLogger.trace("Error searching LoanType by id: {}, error: {}", idLoanType, error.getMessage()));
    }
}
