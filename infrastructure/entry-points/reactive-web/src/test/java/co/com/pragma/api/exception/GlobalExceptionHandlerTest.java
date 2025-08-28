package co.com.pragma.api.exception;

import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.exception.InvalidAmountException;
import co.com.pragma.model.gateways.CustomLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {
    @Mock
    private CustomLogger logger;

    @Mock
    private HandlerFunction<ServerResponse> next;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setup() {
        handler = new GlobalExceptionHandler(logger);
    }

    @Test
    @DisplayName("Should return 400 Bad Request when ValidationException is thrown")
    void shouldHandleValidationException() {
        Map<String, String> errors = Map.of("email", "must not be blank");
        ValidationException ex = new ValidationException(errors);

        when(next.handle(any())).thenReturn(Mono.error(ex));

        StepVerifier.create(handler.filter(mock(ServerRequest.class), next))
                .expectNextMatches(response -> response.statusCode().value() == 400)
                .verifyComplete();

        verify(logger).warn(contains("Validation error"));
    }

    @Test
    @DisplayName("Should return 409 Conflict when InvalidAmountException is thrown")
    void shouldHandleInvalidAmountException() {
        InvalidAmountException ex = new InvalidAmountException("Invalid amount");

        when(next.handle(any())).thenReturn(Mono.error(ex));

        StepVerifier.create(handler.filter(mock(ServerRequest.class), next))
                .expectNextMatches(response -> response.statusCode().value() == 409)
                .verifyComplete();

        verify(logger).warn(contains("Amount invalid"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when EntityNotFoundException is thrown")
    void shouldHandleEntityNotFoundException() {
        EntityNotFoundException ex = new EntityNotFoundException("Entity not found");

        when(next.handle(any())).thenReturn(Mono.error(ex));

        StepVerifier.create(handler.filter(mock(ServerRequest.class), next))
                .expectNextMatches(response -> response.statusCode().value() == 404)
                .verifyComplete();

        verify(logger).warn(contains("Entity not found"));
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error when unexpected exception is thrown")
    void shouldHandleGenericException() {
        RuntimeException ex = new RuntimeException("Something went wrong");

        when(next.handle(any())).thenReturn(Mono.error(ex));

        StepVerifier.create(handler.filter(mock(ServerRequest.class), next))
                .expectNextMatches(response -> response.statusCode().value() == 500)
                .verifyComplete();

        verify(logger).error(contains("Internal server error"));
    }
}
