package co.com.pragma.api.exception;

import co.com.pragma.api.dto.response.ApiErrorResponse;
import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.exception.InvalidAmountException;
import co.com.pragma.model.gateways.CustomLogger;
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
public class GlobalExceptionHandler implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private final CustomLogger logger;

    @Override
    @NonNull
    public Mono<ServerResponse> filter(@NonNull ServerRequest request,
                                       @NonNull HandlerFunction<ServerResponse> next) {
        return next.handle(request)
                .onErrorResume(ValidationException.class, ex -> {
                    logger.warn("Validation error at: " + ex.getMessage());
                    List<ApiErrorResponse.FieldError> fieldErrors = ex.getErrors().entrySet().stream()
                            .map(e -> new ApiErrorResponse.FieldError(e.getKey(), e.getValue()))
                            .collect(Collectors.toList());

                    ApiErrorResponse response = new ApiErrorResponse(
                            OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            400,
                            "BAD REQUEST",
                            "Validation failed",
                            fieldErrors
                    );
                    return ServerResponse.badRequest().bodyValue(response);
                })
                .onErrorResume(InvalidAmountException.class, ex -> {
                    logger.warn("Amount invalid at: " + ex.getMessage());
                    ApiErrorResponse response = new ApiErrorResponse(
                            OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            400,
                            "BAD REQUEST",
                            ex.getMessage(),
                            null
                    );
                    return ServerResponse.status(409).bodyValue(response);
                })
                .onErrorResume(EntityNotFoundException.class, ex -> {
                    logger.warn("Entity not found at: " + ex.getMessage());
                    ApiErrorResponse response = new ApiErrorResponse(
                            OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            404,
                            "NOT FOUND",
                            ex.getMessage(),
                            null
                    );
                    return ServerResponse.status(404).bodyValue(response);
                })
                .onErrorResume(ex -> {
                    logger.error("Internal server error at: " + ex.getMessage());
                    ApiErrorResponse response = new ApiErrorResponse(
                            OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                            500,
                            "INTERNAL SERVER ERROR",
                            ex.getMessage(),
                            null
                    );
                    return ServerResponse.status(500).bodyValue(response);
                });
    }
}
