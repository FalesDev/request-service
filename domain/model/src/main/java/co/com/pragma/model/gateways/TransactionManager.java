package co.com.pragma.model.gateways;

import reactor.core.publisher.Mono;

public interface TransactionManager {
    <T> Mono<T> executeInTransaction(Mono<T> action);
}
