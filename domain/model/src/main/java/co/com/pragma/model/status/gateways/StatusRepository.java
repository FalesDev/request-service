package co.com.pragma.model.status.gateways;

import co.com.pragma.model.status.Status;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface StatusRepository {
    Mono<Status> findById(UUID id);
    Mono<Status> findByName(String name);
    Mono<Status> findByNameIgnoreCase(String name);
    Flux<Status> findByNames(List<String> names);
}
