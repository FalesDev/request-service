package co.com.pragma.r2dbc;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.pagination.CustomPageable;
import co.com.pragma.r2dbc.entity.ApplicationEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
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
    private CustomPageable customPageable;

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
                .idUser(UUID.randomUUID())
                .build();

        entity = new ApplicationEntity(
                domain.getId(),
                domain.getAmount(),
                domain.getTerm(),
                domain.getEmail(),
                domain.getIdDocument(),
                domain.getIdStatus(),
                domain.getIdLoanType(),
                domain.getIdUser()
        );

        customPageable = CustomPageable.builder()
                .page(0)
                .size(10)
                .sortBy("id")
                .sortDirection("asc")
                .build();
    }

    @Test
    @DisplayName("Should return saved application when save succeeds")
    void saveShouldReturnSavedApplication() {
        when(mapper.map(domain, ApplicationEntity.class)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(Mono.just(entity));
        when(mapper.map(entity, Application.class)).thenReturn(domain);

        StepVerifier.create(repositoryAdapter.save(domain))
                .expectNextMatches(app -> app.getId().equals(domain.getId()))
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

    @Test
    @DisplayName("Should find applications by status IDs with pagination")
    void findByIdStatusInShouldReturnCustomPage() {
        List<UUID> statusIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        List<ApplicationEntity> entityList = List.of(entity);
        long totalCount = 1L;

        Pageable pageable = PageRequest.of(customPageable.getPage(), customPageable.getSize(),
                Sort.by(Sort.Direction.ASC, customPageable.getSortBy()));

        when(repository.findByIdStatusIn(eq(statusIds), eq(pageable)))
                .thenReturn(Flux.fromIterable(entityList));
        when(repository.countByIdStatusIn(statusIds)).thenReturn(Mono.just(totalCount));
        when(mapper.map(entity, Application.class)).thenReturn(domain);

        StepVerifier.create(repositoryAdapter.findByIdStatusIn(statusIds, customPageable))
                .expectNextMatches(customPage ->
                        customPage.getContent().size() == 1 &&
                                customPage.getContent().getFirst().equals(domain) &&
                                customPage.getTotalElements() == totalCount &&
                                customPage.getCurrentPage() == customPageable.getPage() &&
                                customPage.getPageSize() == customPageable.getSize())
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle empty results when finding applications by status IDs")
    void findByIdStatusInShouldHandleEmptyResults() {
        List<UUID> statusIds = List.of(UUID.randomUUID());
        long totalCount = 0L;

        Pageable pageable = PageRequest.of(customPageable.getPage(), customPageable.getSize(),
                Sort.by(Sort.Direction.ASC, customPageable.getSortBy()));

        when(repository.findByIdStatusIn(eq(statusIds), eq(pageable)))
                .thenReturn(Flux.empty());
        when(repository.countByIdStatusIn(statusIds)).thenReturn(Mono.just(totalCount));

        StepVerifier.create(repositoryAdapter.findByIdStatusIn(statusIds, customPageable))
                .expectNextMatches(customPage ->
                        customPage.getContent().isEmpty() &&
                                customPage.getTotalElements() == totalCount)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find applications by user ID and status ID")
    void findByIdUserAndIdStatusShouldReturnApplications() {
        UUID userId = UUID.randomUUID();
        UUID statusId = UUID.randomUUID();

        when(repository.findByIdUserAndIdStatus(userId, statusId))
                .thenReturn(Flux.just(entity));
        when(mapper.map(entity, Application.class)).thenReturn(domain);

        StepVerifier.create(repositoryAdapter.findByIdUserAndIdStatus(userId, statusId))
                .expectNextMatches(application ->
                        application.getId().equals(domain.getId()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle empty results when finding applications by user ID and status ID")
    void findByIdUserAndIdStatusShouldHandleEmptyResults() {
        UUID userId = UUID.randomUUID();
        UUID statusId = UUID.randomUUID();

        when(repository.findByIdUserAndIdStatus(userId, statusId))
                .thenReturn(Flux.empty());

        StepVerifier.create(repositoryAdapter.findByIdUserAndIdStatus(userId, statusId))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle errors when counting applications by status IDs")
    void findByIdStatusInShouldHandleCountError() {
        List<UUID> statusIds = List.of(UUID.randomUUID());
        RuntimeException error = new RuntimeException("Count error");

        Pageable pageable = PageRequest.of(customPageable.getPage(), customPageable.getSize(),
                Sort.by(Sort.Direction.ASC, customPageable.getSortBy()));

        when(repository.findByIdStatusIn(eq(statusIds), eq(pageable)))
                .thenReturn(Flux.just(entity));
        when(repository.countByIdStatusIn(statusIds)).thenReturn(Mono.error(error));
        when(mapper.map(entity, Application.class)).thenReturn(domain);

        StepVerifier.create(repositoryAdapter.findByIdStatusIn(statusIds, customPageable))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("Count error"))
                .verify();
    }
}
