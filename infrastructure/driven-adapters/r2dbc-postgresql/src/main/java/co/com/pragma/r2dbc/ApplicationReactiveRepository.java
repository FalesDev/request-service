package co.com.pragma.r2dbc;

import co.com.pragma.r2dbc.entity.ApplicationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// TODO: This file is just an example, you should delete or modify it
public interface ApplicationReactiveRepository extends ReactiveCrudRepository<ApplicationEntity, UUID>, ReactiveQueryByExampleExecutor<ApplicationEntity> {
    Flux<ApplicationEntity> findByIdStatusIn(List<UUID> statusIds, Pageable pageable);
    Mono<Long> countByIdStatusIn(List<UUID> statusIds);
    Flux<ApplicationEntity> findByIdUserAndIdStatus(UUID idUser, UUID idStatus);
    @Query("SELECT a.* FROM applications a " +
            "JOIN status s ON a.id_status = s.id " +
            "WHERE a.id_user = :idUser AND s.name = 'Approved'")
    Flux<ApplicationEntity> findActiveLoansByIdUser(UUID idUser);
    @Query("SELECT * FROM applications WHERE id_status = :statusId AND approved_at >= :start AND approved_at < :end")
    Flux<ApplicationEntity> findByStatusAndApprovedDateBetween(UUID statusId, LocalDateTime start, LocalDateTime end);
}