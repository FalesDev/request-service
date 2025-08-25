package co.com.pragma.api.exception;

import co.com.pragma.api.dto.response.ApiErrorResponse;
import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.exception.InvalidAmountException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    @Override
    @NonNull
    public Mono<ServerResponse> filter(@NonNull ServerRequest request,
                                       @NonNull HandlerFunction<ServerResponse> next) {
        return next.handle(request)
                .onErrorResume(ValidationException.class, ex -> {
                    log.warn("Validation error at {}: {}", request.path(), ex.getMessage());
                    List<ApiErrorResponse.FieldError> fieldErrors = ex.getErrors().entrySet().stream()
                            .map(e -> new ApiErrorResponse.FieldError(e.getKey(), e.getValue()))
                            .collect(Collectors.toList());

                    ApiErrorResponse response = new ApiErrorResponse(
                            OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            400,
                            "BAD REQUEST",
                            "Validation failed",
                            request.path(),
                            fieldErrors
                    );
                    return ServerResponse.badRequest().bodyValue(response);
                })
                .onErrorResume(InvalidAmountException.class, ex -> {
                    log.warn("Amount invalid at {}: {}", request.path(), ex.getMessage());
                    ApiErrorResponse response = new ApiErrorResponse(
                            OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            400,
                            "BAD REQUEST",
                            ex.getMessage(),
                            request.path(),
                            null
                    );
                    return ServerResponse.status(409).bodyValue(response);
                })
                .onErrorResume(EntityNotFoundException.class, ex -> {
                    log.warn("Entity not found at {}: {}", request.path(), ex.getMessage());
                    ApiErrorResponse response = new ApiErrorResponse(
                            OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            404,
                            "NOT FOUND",
                            ex.getMessage(),
                            request.path(),
                            null
                    );
                    return ServerResponse.status(404).bodyValue(response);
                })
                .onErrorResume(ex -> {
                    log.error("Internal server error at {}: {}", request.path(), ex.getMessage());
                    ApiErrorResponse response = new ApiErrorResponse(
                            OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            500,
                            "INTERNAL SERVER ERROR",
                            ex.getMessage(),
                            request.path(),
                            null
                    );
                    return ServerResponse.status(500).bodyValue(response);
                });
    }
}
