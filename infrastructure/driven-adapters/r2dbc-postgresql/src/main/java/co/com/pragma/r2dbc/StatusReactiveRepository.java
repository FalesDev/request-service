package co.com.pragma.r2dbc;

import co.com.pragma.r2dbc.entity.StatusEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface StatusReactiveRepository extends ReactiveCrudRepository<StatusEntity, UUID>, ReactiveQueryByExampleExecutor<StatusEntity> {
    Mono<StatusEntity> findByName(String name);
    Flux<StatusEntity> findByNameIn(List<String> names);
}
