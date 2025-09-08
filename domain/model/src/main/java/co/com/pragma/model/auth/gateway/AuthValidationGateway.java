package co.com.pragma.model.auth.gateway;

import co.com.pragma.model.auth.UserFound;
import co.com.pragma.model.auth.ValidatedUser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface AuthValidationGateway {
    Mono<ValidatedUser> validateClientUser(String idDocument, String token);
    Flux<UserFound> foundClientByIds(List<UUID> userIds, String token);
}
