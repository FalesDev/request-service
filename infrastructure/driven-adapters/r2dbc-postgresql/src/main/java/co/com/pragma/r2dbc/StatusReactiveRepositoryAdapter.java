package co.com.pragma.r2dbc;

import co.com.pragma.model.status.Status;
import co.com.pragma.model.status.gateways.StatusRepository;
import co.com.pragma.r2dbc.entity.StatusEntity;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Repository
public class StatusReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Status,
        StatusEntity,
        UUID,
        StatusReactiveRepository
        > implements StatusRepository {
    public StatusReactiveRepositoryAdapter(StatusReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.map(d, Status.class));
    }

    @Override
    public Mono<Status> findById(UUID id){
        return super.findById(id);
    }

    @Override
    public Mono<Status> findByName(String name){
        return repository.findByName(name)
                .map(entity->mapper.map(entity, Status.class));
    }

    @Override
    public Mono<Status> findByNameIgnoreCase(String name){
        return repository.findByNameIgnoreCase(name)
                .map(entity->mapper.map(entity, Status.class));
    }

    @Override
    public Flux<Status> findByNames(List<String> names) {
        return repository.findByNameIn(names)
                .map(entity -> mapper.map(entity, Status.class));
    }
}
