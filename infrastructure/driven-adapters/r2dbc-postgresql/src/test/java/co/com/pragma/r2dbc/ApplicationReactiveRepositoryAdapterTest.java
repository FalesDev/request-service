package co.com.pragma.r2dbc;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.pagination.CustomPage;
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

import java.time.LocalDateTime;
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

        entity = ApplicationEntity.builder()
                .id(domain.getId())
                .amount(domain.getAmount())
                .term(domain.getTerm())
                .email(domain.getEmail())
                .idDocument(domain.getIdDocument())
                .idStatus(domain.getIdStatus())
                .idLoanType(domain.getIdLoanType())
                .idUser(domain.getIdUser())
                .build();

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
    @DisplayName("Should find application by ID")
    void findByIdShouldReturnApplication() {
        UUID id = domain.getId();

        when(repository.findById(id)).thenReturn(Mono.just(entity));
        when(mapper.map(entity, Application.class)).thenReturn(domain);

        StepVerifier.create(repositoryAdapter.findById(id))
                .expectNextMatches(app -> app.getId().equals(domain.getId()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should propagate error when repository findById fails")
    void findByIdShouldPropagateError() {
        UUID id = UUID.randomUUID();
        RuntimeException error = new RuntimeException("DB find error");

        when(repository.findById(id)).thenReturn(Mono.error(error));

        StepVerifier.create(repositoryAdapter.findById(id))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("DB find error"))
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

    @Test
    @DisplayName("Should handle DESC sort direction correctly")
    void findByIdStatusInShouldHandleDescSortDirection() {
        List<UUID> statusIds = List.of(UUID.randomUUID());
        List<ApplicationEntity> entityList = List.of(entity);
        long totalCount = 1L;

        CustomPageable customPageable = CustomPageable.builder()
                .page(0)
                .size(10)
                .sortBy("id")
                .sortDirection("desc")
                .build();

        Pageable pageable = PageRequest.of(customPageable.getPage(), customPageable.getSize(),
                Sort.by(Sort.Direction.DESC, customPageable.getSortBy()));

        when(repository.findByIdStatusIn(eq(statusIds), eq(pageable)))
                .thenReturn(Flux.fromIterable(entityList));
        when(repository.countByIdStatusIn(statusIds)).thenReturn(Mono.just(totalCount));
        when(mapper.map(entity, Application.class)).thenReturn(domain);

        StepVerifier.create(repositoryAdapter.findByIdStatusIn(statusIds, customPageable))
                .expectNextMatches(customPage ->
                        customPage.getContent().size() == 1 &&
                                customPage.getTotalElements() == totalCount)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should propagate error when repository findByIdStatusIn fails")
    void findByIdStatusInShouldPropagateRepositoryError() {
        List<UUID> statusIds = List.of(UUID.randomUUID());
        RuntimeException error = new RuntimeException("DB find error");

        Pageable pageable = PageRequest.of(customPageable.getPage(), customPageable.getSize(),
                Sort.by(Sort.Direction.ASC, customPageable.getSortBy()));

        when(repository.findByIdStatusIn(eq(statusIds), eq(pageable)))
                .thenReturn(Flux.error(error));
        when(repository.countByIdStatusIn(statusIds)).thenReturn(Mono.just(0L));

        StepVerifier.create(repositoryAdapter.findByIdStatusIn(statusIds, customPageable))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("DB find error"))
                .verify();
    }

    @Test
    @DisplayName("Should propagate error when repository findByIdUserAndIdStatus fails")
    void findByIdUserAndIdStatusShouldPropagateRepositoryError() {
        UUID userId = UUID.randomUUID();
        UUID statusId = UUID.randomUUID();
        RuntimeException error = new RuntimeException("DB findByUser error");

        when(repository.findByIdUserAndIdStatus(userId, statusId))
                .thenReturn(Flux.error(error));

        StepVerifier.create(repositoryAdapter.findByIdUserAndIdStatus(userId, statusId))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("DB findByUser error"))
                .verify();
    }

    @Test
    @DisplayName("Should correctly calculate hasNext and totalPages for multiple pages")
    void findByIdStatusInMultiplePages() {
        List<UUID> statusIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        List<ApplicationEntity> entityList = List.of(entity, entity, entity);
        long totalCount = 15L;

        Pageable pageable = PageRequest.of(customPageable.getPage(), customPageable.getSize(),
                Sort.by(Sort.Direction.ASC, customPageable.getSortBy()));

        when(repository.findByIdStatusIn(eq(statusIds), eq(pageable)))
                .thenReturn(Flux.fromIterable(entityList));
        when(repository.countByIdStatusIn(statusIds)).thenReturn(Mono.just(totalCount));
        when(mapper.map(entity, Application.class)).thenReturn(domain);

        StepVerifier.create(repositoryAdapter.findByIdStatusIn(statusIds, customPageable))
                .expectNextMatches(customPage -> {
                    int expectedTotalPages = (int) Math.ceil((double) totalCount / customPageable.getSize());
                    return customPage.getContent().size() == 3 &&
                            customPage.getTotalElements() == totalCount &&
                            customPage.getTotalPages() == expectedTotalPages &&
                            customPage.isHasNext() == (customPageable.getPage() < expectedTotalPages - 1);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find active loans by user ID")
    void findActiveLoansByIdUserShouldReturnApplications() {
        UUID userId = UUID.randomUUID();

        when(repository.findActiveLoansByIdUser(userId))
                .thenReturn(Flux.just(entity));
        when(mapper.map(entity, Application.class)).thenReturn(domain);

        StepVerifier.create(repositoryAdapter.findActiveLoansByIdUser(userId))
                .expectNextMatches(application -> application.getId().equals(domain.getId()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle empty results when finding active loans by user ID")
    void findActiveLoansByIdUserShouldHandleEmptyResults() {
        UUID userId = UUID.randomUUID();

        when(repository.findActiveLoansByIdUser(userId))
                .thenReturn(Flux.empty());

        StepVerifier.create(repositoryAdapter.findActiveLoansByIdUser(userId))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should propagate error when repository findActiveLoansByIdUser fails")
    void findActiveLoansByIdUserShouldPropagateRepositoryError() {
        UUID userId = UUID.randomUUID();
        RuntimeException error = new RuntimeException("DB activeLoans error");

        when(repository.findActiveLoansByIdUser(userId))
                .thenReturn(Flux.error(error));

        StepVerifier.create(repositoryAdapter.findActiveLoansByIdUser(userId))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("DB activeLoans error"))
                .verify();
    }

    @Test
    @DisplayName("Should find applications by status and approved date range")
    void findByStatusAndApprovedDateBetweenShouldReturnApplications() {
        UUID statusId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();

        when(repository.findByStatusAndApprovedDateBetween(statusId, start, end))
                .thenReturn(Flux.just(entity));
        when(mapper.map(entity, Application.class)).thenReturn(domain);

        StepVerifier.create(repositoryAdapter.findByStatusAndApprovedDateBetween(statusId, start, end))
                .expectNextMatches(application -> application.getId().equals(domain.getId()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle empty results when finding applications by status and approved date range")
    void findByStatusAndApprovedDateBetweenShouldHandleEmptyResults() {
        UUID statusId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();

        when(repository.findByStatusAndApprovedDateBetween(statusId, start, end))
                .thenReturn(Flux.empty());

        StepVerifier.create(repositoryAdapter.findByStatusAndApprovedDateBetween(statusId, start, end))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should propagate error when repository findByStatusAndApprovedDateBetween fails")
    void findByStatusAndApprovedDateBetweenShouldPropagateRepositoryError() {
        UUID statusId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        RuntimeException error = new RuntimeException("DB dateRange error");

        when(repository.findByStatusAndApprovedDateBetween(statusId, start, end))
                .thenReturn(Flux.error(error));

        StepVerifier.create(repositoryAdapter.findByStatusAndApprovedDateBetween(statusId, start, end))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("DB dateRange error"))
                .verify();
    }

    @Test
    @DisplayName("Should correctly set hasPrevious when current page is greater than 0")
    void findByIdStatusInShouldSetHasPreviousTrue() {
        List<UUID> statusIds = List.of(UUID.randomUUID());
        List<ApplicationEntity> entityList = List.of(entity);
        long totalCount = 10L;

        CustomPageable customPageableWithPage = CustomPageable.builder()
                .page(1)
                .size(5)
                .sortBy("id")
                .sortDirection("asc")
                .build();

        Pageable pageable = PageRequest.of(customPageableWithPage.getPage(), customPageableWithPage.getSize(),
                Sort.by(Sort.Direction.ASC, customPageableWithPage.getSortBy()));

        when(repository.findByIdStatusIn(eq(statusIds), eq(pageable)))
                .thenReturn(Flux.fromIterable(entityList));
        when(repository.countByIdStatusIn(statusIds)).thenReturn(Mono.just(totalCount));
        when(mapper.map(entity, Application.class)).thenReturn(domain);

        StepVerifier.create(repositoryAdapter.findByIdStatusIn(statusIds, customPageableWithPage))
                .expectNextMatches(CustomPage::isHasPrevious)
                .verifyComplete();
    }

}
