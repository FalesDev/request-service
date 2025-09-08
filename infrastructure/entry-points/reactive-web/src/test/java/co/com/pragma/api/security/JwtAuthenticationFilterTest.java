package co.com.pragma.api.security;

import co.com.pragma.model.auth.ValidatedUser;
import co.com.pragma.model.exception.TokenValidationException;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.gateways.TokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private TokenValidator tokenValidator;

    @Mock
    private CustomLogger logger;

    @Mock
    private WebFilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(tokenValidator, logger);
    }

    @Test
    void shouldContinueChainWhenNoToken() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test").build()
        );

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(tokenValidator, never()).validateToken(anyString());
    }

    @Test
    void shouldAuthenticateWhenTokenIsValid() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .build()
        );

        when(tokenValidator.validateToken("valid-token"))
                .thenReturn(Mono.just(new ValidatedUser(UUID.randomUUID(),
                        "user@email.com", "12345678", "USER")));

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(tokenValidator).validateToken("valid-token");
    }

    @Test
    void shouldHandleTokenValidationException() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token")
                        .build()
        );

        when(tokenValidator.validateToken("bad-token"))
                .thenReturn(Mono.error(new TokenValidationException("Invalid token")));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getAttributes())
                .containsEntry("AUTH_ERROR", "Invalid token");

        verify(logger).warn(eq("Token validation failed: {}"), eq("Invalid token"));
    }

    @Test
    void shouldHandleGenericException() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token")
                        .build()
        );

        when(tokenValidator.validateToken("bad-token"))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getAttributes()).doesNotContainKey("AUTH_ERROR");

        verify(logger, never()).warn(anyString(), any());
    }
}

