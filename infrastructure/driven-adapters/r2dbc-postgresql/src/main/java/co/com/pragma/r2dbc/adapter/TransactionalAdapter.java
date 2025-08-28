package co.com.pragma.r2dbc.adapter;

import co.com.pragma.model.gateways.TransactionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TransactionalAdapter implements TransactionManager {

    private final TransactionalOperator transactionalOperator;

    @Override
    public <T> Mono<T> executeInTransaction(Mono<T> action) {
        return transactionalOperator.transactional(action);
    }
}
