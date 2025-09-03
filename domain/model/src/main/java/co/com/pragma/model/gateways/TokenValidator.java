package co.com.pragma.model.gateways;

import co.com.pragma.model.auth.ValidatedUser;
import reactor.core.publisher.Mono;

public interface TokenValidator {
    Mono<ValidatedUser> validateToken(String token);
}
