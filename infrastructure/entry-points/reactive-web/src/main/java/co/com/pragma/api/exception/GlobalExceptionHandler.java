package co.com.pragma.api.exception;

import co.com.pragma.api.dto.response.ApiErrorResponse;
import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.exception.InvalidAmountException;
import co.com.pragma.model.exception.TokenValidationException;
import co.com.pragma.model.exception.UnauthorizedException;
import co.com.pragma.model.gateways.CustomLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class GlobalExceptionHandler implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private final CustomLogger logger;

    @Override
    @NonNull
    public Mono<ServerResponse> filter(@NonNull ServerRequest request,
                                       @NonNull HandlerFunction<ServerResponse> next) {
        return next.handle(request)
                .onErrorResume(ValidationException.class, ex -> {
                    logger.warn("Validation error at: " + ex.getErrors());
                    ApiErrorResponse response = ApiErrorResponse.builder()
                            .timestamp( OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                            .status(HttpStatus.BAD_REQUEST.value())
                            .error(HttpStatus.BAD_REQUEST.name())
                            .message("Validation failed")
                            .errors(ex.getErrors())
                            .build();
                    return ServerResponse.badRequest().bodyValue(response);
                })
                .onErrorResume(InvalidAmountException.class, ex -> {
                    logger.warn("Amount invalid at: " + ex.getMessage());
                    ApiErrorResponse response = ApiErrorResponse.builder()
                            .timestamp(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                            .status(HttpStatus.BAD_REQUEST.value())
                            .error(HttpStatus.BAD_REQUEST.name())
                            .message(ex.getMessage())
                            .build();
                    return ServerResponse.status(HttpStatus.BAD_REQUEST.value()).bodyValue(response);
                })
                .onErrorResume(EntityNotFoundException.class, ex -> {
                    logger.warn("Entity not found at: " + ex.getMessage());
                    ApiErrorResponse response = ApiErrorResponse.builder()
                            .timestamp(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                            .status(HttpStatus.NOT_FOUND.value())
                            .error(HttpStatus.NOT_FOUND.name())
                            .message(ex.getMessage())
                            .build();
                    return ServerResponse.status(HttpStatus.NOT_FOUND.value()).bodyValue(response);
                })
                .onErrorResume(UnauthorizedException.class, ex -> {
                    logger.warn("Authentication failed: " + ex.getMessage());
                    ApiErrorResponse response = ApiErrorResponse.builder()
                            .timestamp(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                            .status(HttpStatus.UNAUTHORIZED.value())
                            .error(HttpStatus.UNAUTHORIZED.name())
                            .message(ex.getMessage())
                            .build();
                    return ServerResponse.status(HttpStatus.UNAUTHORIZED.value()).bodyValue(response);
                })
                .onErrorResume(TokenValidationException.class, ex -> {
                    logger.warn("JWT validation failed: " + ex.getMessage());
                    ApiErrorResponse response = ApiErrorResponse.builder()
                            .timestamp(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                            .status(HttpStatus.UNAUTHORIZED.value())
                            .error(HttpStatus.UNAUTHORIZED.name())
                            .message(ex.getMessage())
                            .build();
                    return ServerResponse.status(HttpStatus.UNAUTHORIZED.value()).bodyValue(response);
                })
                .onErrorResume(ex -> {
                    logger.error("Internal server error at: " + ex.getMessage());
                    ApiErrorResponse response = ApiErrorResponse.builder()
                            .timestamp(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .error(HttpStatus.INTERNAL_SERVER_ERROR.name())
                            .message(ex.getMessage())
                            .build();
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).bodyValue(response);
                });
    }
}
