package co.com.pragma.model.gateways;

import co.com.pragma.model.auth.ValidatedUser;
import reactor.core.publisher.Mono;

public interface AuthValidationGateway {
    Mono<ValidatedUser> validateClientUser(String idDocument, String token);
}
