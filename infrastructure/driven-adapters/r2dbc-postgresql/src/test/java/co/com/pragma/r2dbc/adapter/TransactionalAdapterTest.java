package co.com.pragma.r2dbc.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionalAdapterTest {

    @Mock
    private TransactionalOperator transactionalOperator;

    private TransactionalAdapter transactionalAdapter;

    @BeforeEach
    void setUp() {
        transactionalAdapter = new TransactionalAdapter(transactionalOperator);
    }

    @Test
    @DisplayName("Should return success result when action completes successfully")
    void executeInTransactionShouldReturnSuccessResult() {
        String expectedResult = "Success";
        Mono<String> action = Mono.just(expectedResult);

        when(transactionalOperator.transactional(Mockito.<Mono<String>>any()))
                .thenReturn(action);

        Mono<String> result = transactionalAdapter.executeInTransaction(action);

        StepVerifier.create(result)
                .expectNext(expectedResult)
                .verifyComplete();

        verify(transactionalOperator, times(1)).transactional(action);
    }

    @Test
    @DisplayName("Should return error result when action fails within transaction")
    void executeInTransactionShouldReturnErrorResult() {
        RuntimeException expectedException = new RuntimeException("Transaction failed");
        Mono<String> failingAction = Mono.error(expectedException);

        when(transactionalOperator.transactional(Mockito.<Mono<String>>any()))
                .thenReturn(failingAction);

        Mono<String> result = transactionalAdapter.executeInTransaction(failingAction);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("Transaction failed"))
                .verify();

        verify(transactionalOperator, times(1)).transactional(failingAction);
    }



    @Test
    @DisplayName("Should complete without emitting values when action is empty")
    void executeInTransactionWithEmptyMonoShouldComplete() {
        Mono<String> emptyAction = Mono.empty();
        when(transactionalOperator.transactional(Mockito.<Mono<String>>any()))
                .thenReturn(emptyAction);

        Mono<String> result = transactionalAdapter.executeInTransaction(emptyAction);

        StepVerifier.create(result)
                .verifyComplete();

        verify(transactionalOperator, times(1)).transactional(emptyAction);
    }
}
