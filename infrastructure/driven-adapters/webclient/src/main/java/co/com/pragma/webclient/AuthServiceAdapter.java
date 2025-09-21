package co.com.pragma.webclient;

import co.com.pragma.model.auth.UserFound;
import co.com.pragma.model.auth.ValidatedUser;
import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.exception.UnauthorizedException;
import co.com.pragma.model.auth.gateway.AuthValidationGateway;
import co.com.pragma.webclient.dto.UserValidationRequest;
import co.com.pragma.webclient.dto.UsersFoundRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthServiceAdapter implements AuthValidationGateway {

    private final WebClient authWebClient;

    @Override
    public Mono<ValidatedUser> validateClientUser(String idDocument, String token){
        return authWebClient
                .post()
                .uri("/auth/api/v1/users/document")
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

    @Override
    public Flux<UserFound> foundClientByIds(List<UUID> userIds, String token) {
        return authWebClient
                .post()
                .uri("/auth/api/v1/users/find")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(new UsersFoundRequest(userIds))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    if (response.statusCode().equals(HttpStatus.UNAUTHORIZED)) {
                        return Mono.error(new UnauthorizedException("Unauthorized: Invalid token"));
                    }
                    if (response.statusCode().equals(HttpStatus.NOT_FOUND)) {
                        return Mono.error(new EntityNotFoundException("Users not found in auth service"));
                    }
                    return Mono.empty();
                })
                .bodyToFlux(UserFound.class);
    }
}
