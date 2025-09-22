package co.com.pragma.r2dbc;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.application.gateways.ApplicationRepository;
import co.com.pragma.model.pagination.CustomPage;
import co.com.pragma.model.pagination.CustomPageable;
import co.com.pragma.r2dbc.entity.ApplicationEntity;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class ApplicationReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Application,
        ApplicationEntity,
        UUID,
        ApplicationReactiveRepository
> implements ApplicationRepository {
    public ApplicationReactiveRepositoryAdapter(ApplicationReactiveRepository repository,
                                                ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.map(d, Application.class));
    }

    @Override
    public Mono<Application> save(Application application) {
        return super.save(application);
    }

    @Override
    public Mono<Application> findById(UUID id){
        return super.findById(id);
    }

    @Override
    public Mono<CustomPage<Application>> findByIdStatusIn(List<UUID> statusIds, CustomPageable customPageable) {
        Pageable pageable = convertToPageable(customPageable);

        return repository.findByIdStatusIn(statusIds, pageable)
                .map(entity -> mapper.map(entity, Application.class))
                .collectList()
                .zipWith(repository.countByIdStatusIn(statusIds))
                .map(tuple -> {
                    List<Application> applications = tuple.getT1();
                    long totalElements = tuple.getT2();
                    int totalPages = (int) Math.ceil((double) totalElements / customPageable.getSize());

                    return CustomPage.<Application>builder()
                            .content(applications)
                            .currentPage(customPageable.getPage())
                            .totalPages(totalPages)
                            .totalElements(totalElements)
                            .pageSize(customPageable.getSize())
                            .hasNext(customPageable.getPage() < totalPages - 1)
                            .hasPrevious(customPageable.getPage() > 0)
                            .build();
                });
    }

    @Override
    public Flux<Application> findByIdUserAndIdStatus(UUID userId, UUID statusId) {
        return repository.findByIdUserAndIdStatus(userId, statusId)
                .map(entity -> mapper.map(entity, Application.class));
    }

    @Override
    public Flux<Application> findActiveLoansByIdUser(UUID userId) {
        return repository.findActiveLoansByIdUser(userId)
                .map(entity -> mapper.map(entity, Application.class));
    }

    @Override
    public Flux<Application> findByStatusAndApprovedDateBetween(UUID statusId, LocalDateTime start, LocalDateTime end) {
        return repository.findByStatusAndApprovedDateBetween(statusId, start, end)
                .map(entity -> mapper.map(entity, Application.class));
    }

    private Pageable convertToPageable(CustomPageable customPageable) {
        Sort.Direction direction = Sort.Direction.fromString(
                customPageable.getSortDirection().equalsIgnoreCase("desc") ? "DESC" : "ASC"
        );
        Sort sort = Sort.by(direction, customPageable.getSortBy());
        return PageRequest.of(customPageable.getPage(), customPageable.getSize(), sort);
    }
}
