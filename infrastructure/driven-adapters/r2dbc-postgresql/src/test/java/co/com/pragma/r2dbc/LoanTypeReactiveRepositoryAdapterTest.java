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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
}
