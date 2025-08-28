package co.com.pragma.r2dbc;

import co.com.pragma.model.application.Application;
import co.com.pragma.r2dbc.entity.ApplicationEntity;
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
class ApplicationReactiveRepositoryAdapterTest {

    @InjectMocks
    ApplicationReactiveRepositoryAdapter repositoryAdapter;

    @Mock
    ApplicationReactiveRepository repository;

    @Mock
    ObjectMapper mapper;

    private Application domain;
    private ApplicationEntity entity;

    @BeforeEach
    void setup() {
        domain = Application.builder()
                .id(UUID.randomUUID())
                .amount(20000.0)
                .term(12)
                .email("test@example.com")
                .idDocument("99999999")
                .idStatus(UUID.randomUUID())
                .idLoanType(UUID.randomUUID())
                .build();

        entity = new ApplicationEntity(
                domain.getId(),
                domain.getAmount(),
                domain.getTerm(),
                domain.getEmail(),
                domain.getIdDocument(),
                domain.getIdStatus(),
                domain.getIdLoanType()
        );
    }

    @Test
    @DisplayName("Should return saved application when save succeeds")
    void saveShouldReturnSavedApplication() {
        when(mapper.map(domain, ApplicationEntity.class)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(Mono.just(entity));
        when(mapper.map(entity, Application.class)).thenReturn(domain);

        StepVerifier.create(repositoryAdapter.save(domain))
                .expectNextMatches(user -> user.getId().equals(domain.getId()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should propagate error when repository save fails")
    void saveShouldPropagateErrorWhenRepositoryFails() {
        RuntimeException error = new RuntimeException("DB error");
        when(mapper.map(domain, ApplicationEntity.class)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(Mono.error(error));

        StepVerifier.create(repositoryAdapter.save(domain))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("DB error"))
                .verify();
    }
}
