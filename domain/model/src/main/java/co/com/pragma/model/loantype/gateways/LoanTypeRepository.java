package co.com.pragma.model.loantype.gateways;

import co.com.pragma.model.loantype.LoanType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface LoanTypeRepository {
    Mono<LoanType> findById(UUID id);
    Flux<LoanType> findByIds(List<UUID> ids);
    Mono<LoanType> findByName(String name);
}
