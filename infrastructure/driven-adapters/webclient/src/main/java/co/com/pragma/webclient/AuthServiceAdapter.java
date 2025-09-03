package co.com.pragma.webclient;

import co.com.pragma.model.auth.ValidatedUser;
import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.exception.UnauthorizedException;
import co.com.pragma.model.gateways.AuthValidationGateway;
import co.com.pragma.webclient.dto.UserValidationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthServiceAdapter implements AuthValidationGateway {

    private final WebClient authWebClient;

    @Override
    public Mono<ValidatedUser> validateClientUser(String idDocument, String token){
        return authWebClient
                .post()
                .uri("/api/v1/users/document")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(new UserValidationRequest(idDocument))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    if (response.statusCode().equals(HttpStatus.UNAUTHORIZED)) {
                        return Mono.error(new UnauthorizedException("Unauthorized: Invalid token"));
                    }
                    if (response.statusCode().equals(HttpStatus.NOT_FOUND)) {
                        return Mono.error(new EntityNotFoundException("User not found in auth service"));
                    }
                    return Mono.empty();
                })
                .bodyToMono(ValidatedUser.class);
    }
}
