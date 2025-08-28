package co.com.pragma.r2dbc;

import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.status.Status;
import co.com.pragma.r2dbc.entity.LoanTypeEntity;
import co.com.pragma.r2dbc.entity.StatusEntity;
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
public class StatusReactiveRepositoryAdapterTest {

    @InjectMocks
    StatusReactiveRepositoryAdapter repositoryAdapter;

    @Mock
    StatusReactiveRepository repository;

    @Mock
    ObjectMapper mapper;

    private Status domain;
    private StatusEntity entity;

    @BeforeEach
    void setup() {
        domain = Status.builder()
                .id(UUID.randomUUID())
                .name("Pending Review")
                .description("Description")
                .build();

        entity = new StatusEntity(
                domain.getId(),
                domain.getName(),
                domain.getDescription()
        );
    }

    @Test
    @DisplayName("Should return Status when found by name")
    void shouldFindByName() {
        when(repository.findByName(domain.getName())).thenReturn(Mono.just(entity));
        when(mapper.map(entity, Status.class)).thenReturn(domain);

        StepVerifier.create(repositoryAdapter.findByName(domain.getName()))
                .expectNext(domain)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty Mono when Status not found by name")
    void shouldReturnEmptyWhenNotFoundByName() {
        when(repository.findByName("NotExist")).thenReturn(Mono.empty());

        StepVerifier.create(repositoryAdapter.findByName("NotExist"))
                .verifyComplete();
    }
}
