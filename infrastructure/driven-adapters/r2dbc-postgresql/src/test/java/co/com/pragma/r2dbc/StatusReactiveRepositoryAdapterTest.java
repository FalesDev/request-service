package co.com.pragma.r2dbc;

import co.com.pragma.model.status.Status;
import co.com.pragma.r2dbc.entity.StatusEntity;
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
public class StatusReactiveRepositoryAdapterTest {

    @InjectMocks
    StatusReactiveRepositoryAdapter repositoryAdapter;

    @Mock
    StatusReactiveRepository repository;

    @Mock
    ObjectMapper mapper;

    private Status domain;
    private StatusEntity entity;
    private Status domain2;
    private StatusEntity entity2;

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

        domain2 = Status.builder()
                .id(UUID.randomUUID())
                .name("Approved")
                .description("Approved description")
                .build();

        entity2 = new StatusEntity(
                domain2.getId(),
                domain2.getName(),
                domain2.getDescription()
        );
    }

    @Test
    @DisplayName("Should return Status when found by id")
    void shouldFindById() {
        when(repository.findById(domain.getId())).thenReturn(Mono.just(entity));
        when(mapper.map(entity, Status.class)).thenReturn(domain);

        StepVerifier.create(repositoryAdapter.findById(domain.getId()))
                .expectNext(domain)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty Mono when Status not found by id")
    void shouldReturnEmptyWhenNotFoundById() {
        UUID randomId = UUID.randomUUID();
        when(repository.findById(randomId)).thenReturn(Mono.empty());

        StepVerifier.create(repositoryAdapter.findById(randomId))
                .verifyComplete();
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

    @Test
    @DisplayName("Should return Statuses when found by names")
    void shouldFindByNames() {
        List<String> names = List.of("Pending Review", "Approved");
        List<StatusEntity> entityList = List.of(entity, entity2);
        List<Status> domainList = List.of(domain, domain2);

        when(repository.findByNameIn(names)).thenReturn(Flux.fromIterable(entityList));
        when(mapper.map(entity, Status.class)).thenReturn(domain);
        when(mapper.map(entity2, Status.class)).thenReturn(domain2);

        StepVerifier.create(repositoryAdapter.findByNames(names))
                .expectNext(domain, domain2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty Flux when no Statuses found by names")
    void shouldReturnEmptyWhenNotFoundByNames() {
        List<String> names = List.of("NonExistentStatus");
        when(repository.findByNameIn(names)).thenReturn(Flux.empty());

        StepVerifier.create(repositoryAdapter.findByNames(names))
                .verifyComplete();
    }
}
