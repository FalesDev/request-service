package co.com.pragma.r2dbc;

import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.r2dbc.entity.LoanTypeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoanTypeReactiveRepositoryAdapterTest {

    @InjectMocks
    LoanTypeReactiveRepositoryAdapter repositoryAdapter;

    @Mock
    LoanTypeReactiveRepository repository;

    @Mock
    ObjectMapper mapper;

    private LoanType domain;
    private LoanTypeEntity entity;

    @BeforeEach
    void setup() {
        domain = LoanType.builder()
                .id(UUID.randomUUID())
                .name("Loan Type")
                .minAmount(10000.0)
                .maxAmount(49999.99)
                .interestRate(8.3)
                .automaticValidation(true)
                .build();

        entity = new LoanTypeEntity(
                domain.getId(),
                domain.getName(),
                domain.getMinAmount(),
                domain.getMaxAmount(),
                domain.getInterestRate(),
                domain.getAutomaticValidation()
        );
    }

    @Test
    @DisplayName("Should return LoanType when found by id")
    void shouldFindById() {
        when(repository.findById(domain.getId())).thenReturn(Mono.just(entity));
        when(mapper.map(entity, LoanType.class)).thenReturn(domain);

        StepVerifier.create(repositoryAdapter.findById(domain.getId()))
                .expectNext(domain)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty Mono when LoanType not found by id")
    void shouldReturnEmptyWhenNotFoundById() {
        UUID randomId = UUID.randomUUID();
        when(repository.findById(randomId)).thenReturn(Mono.empty());

        StepVerifier.create(repositoryAdapter.findById(randomId))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return LoanType when found by name")
    void shouldFindByName() {
        when(repository.findByName(domain.getName())).thenReturn(Mono.just(entity));
        when(mapper.map(entity, LoanType.class)).thenReturn(domain);

        StepVerifier.create(repositoryAdapter.findByName(domain.getName()))
                .expectNext(domain)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty Mono when LoanType not found by name")
    void shouldReturnEmptyWhenNotFoundByName() {
        when(repository.findByName("NotExist")).thenReturn(Mono.empty());

        StepVerifier.create(repositoryAdapter.findByName("NotExist"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return LoanTypes when found by ids")
    void shouldFindByIds() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        LoanTypeEntity entity1 = new LoanTypeEntity(id1, "Personal Loan", 5000.0, 20000.0, 7.5, true);
        LoanTypeEntity entity2 = new LoanTypeEntity(id2, "Car Loan", 10000.0, 40000.0, 6.0, false);

        LoanType domain1 = LoanType.builder()
                .id(id1).name("Personal Loan").minAmount(5000.0).maxAmount(20000.0)
                .interestRate(7.5).automaticValidation(true).build();
        LoanType domain2 = LoanType.builder()
                .id(id2).name("Car Loan").minAmount(10000.0).maxAmount(40000.0)
                .interestRate(6.0).automaticValidation(false).build();

        when(repository.findAllById(List.of(id1, id2)))
                .thenReturn(Flux.just(entity1, entity2));
        when(mapper.map(entity1, LoanType.class)).thenReturn(domain1);
        when(mapper.map(entity2, LoanType.class)).thenReturn(domain2);

        StepVerifier.create(repositoryAdapter.findByIds(List.of(id1, id2)))
                .expectNext(domain1)
                .expectNext(domain2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty Flux when no LoanTypes found by ids")
    void shouldReturnEmptyWhenNotFoundByIds() {
        UUID id = UUID.randomUUID();

        when(repository.findAllById(List.of(id)))
                .thenReturn(Flux.empty());

        StepVerifier.create(repositoryAdapter.findByIds(List.of(id)))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should propagate error when repository findAllById fails")
    void shouldPropagateErrorWhenFindByIdsFails() {
        UUID id = UUID.randomUUID();
        RuntimeException error = new RuntimeException("DB error");

        when(repository.findAllById(List.of(id)))
                .thenReturn(Flux.error(error));

        StepVerifier.create(repositoryAdapter.findByIds(List.of(id)))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("DB error"))
                .verify();
    }
}
