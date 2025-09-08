package co.com.pragma.model.application.gateways;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.pagination.CustomPage;
import co.com.pragma.model.pagination.CustomPageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;


public interface ApplicationRepository {
    Mono<Application> save(Application application);
    Mono<CustomPage<Application>> findByIdStatusIn(List<UUID> statusIds, CustomPageable pageable);
    Flux<Application> findByIdUserAndIdStatus(UUID userId, UUID statusId);
}
