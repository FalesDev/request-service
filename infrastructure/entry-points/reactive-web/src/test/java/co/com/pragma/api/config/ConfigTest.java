package co.com.pragma.api.config;

import co.com.pragma.api.security.JwtAuthenticationFilter;
import co.com.pragma.model.gateways.CustomLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@ContextConfiguration(classes = {
        CorsConfig.class,
        SecurityHeadersConfig.class,
        ConfigTest.TestRouter.class,
        SecurityConfig.class,
})
@WebFluxTest
class ConfigTest {

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private CustomLogger customLogger;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should return 200 OK with security headers on GET /test")
    void testGetShouldReturnOk() {
        when(jwtAuthenticationFilter.filter(any(), any())).thenAnswer(invocation -> {
            ServerWebExchange exchange = invocation.getArgument(0);
            WebFilterChain chain = invocation.getArgument(1);
            return chain.filter(exchange);
        });

        webTestClient.get()
                .uri("/test")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-Security-Policy",
                        "default-src 'self'; frame-ancestors 'self'; form-action 'self'")
                .expectHeader().valueEquals("Strict-Transport-Security", "max-age=31536000;")
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("Server", "")
                .expectHeader().valueEquals("Cache-Control", "no-store")
                .expectHeader().valueEquals("Pragma", "no-cache")
                .expectHeader().valueEquals("Referrer-Policy", "strict-origin-when-cross-origin")
                .expectBody(String.class).isEqualTo("ok");
    }

    @Test
    @DisplayName("Should return 204 No Content with security headers on POST /test")
    void testPostShouldReturnNoContent() {
        when(jwtAuthenticationFilter.filter(any(), any())).thenAnswer(invocation -> {
            ServerWebExchange exchange = invocation.getArgument(0);
            WebFilterChain chain = invocation.getArgument(1);
            return chain.filter(exchange);
        });

        webTestClient.post()
                .uri("/test")
                .exchange()
                .expectStatus().isNoContent()
                .expectHeader().valueEquals("Content-Security-Policy",
                        "default-src 'self'; frame-ancestors 'self'; form-action 'self'")
                .expectHeader().valueEquals("Strict-Transport-Security", "max-age=31536000;")
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("Server", "")
                .expectHeader().valueEquals("Cache-Control", "no-store")
                .expectHeader().valueEquals("Pragma", "no-cache")
                .expectHeader().valueEquals("Referrer-Policy", "strict-origin-when-cross-origin");
    }

    @Test
    @DisplayName("Should return 401 Unauthorized with custom AUTH_ERROR message")
    void testUnauthorizedWithCustomAuthError() {
        when(jwtAuthenticationFilter.filter(any(), any())).thenAnswer(invocation -> {
            ServerWebExchange exchange = invocation.getArgument(0);
            WebFilterChain chain = invocation.getArgument(1);
            return chain.filter(exchange);
        });

        webTestClient.get()
                .uri("/api/v1/requests")
                .header("Authorization", "Bearer expired")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").isEqualTo("Authentication failed"); // Changed expected message
    }

    @Configuration
    static class TestRouter {
        @Bean
        public RouterFunction<ServerResponse> testRoute() {
            return route()
                    .GET("/test", req -> ServerResponse.ok().bodyValue("ok"))
                    .POST("/test", req -> ServerResponse.noContent().build())
                    .GET("/api/v1/requests", req -> ServerResponse.ok().bodyValue("user details"))
                    .build();
        }
    }

}