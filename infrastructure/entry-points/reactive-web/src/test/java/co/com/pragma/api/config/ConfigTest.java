package co.com.pragma.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@ContextConfiguration(classes = {
        CorsConfig.class,
        SecurityHeadersConfig.class,
        ConfigTest.TestRouter.class
})
@WebFluxTest
class ConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Should return 200 OK with security headers on GET /test")
    void testGetShouldReturnOk() {
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
                .expectBody(String.class)
                .consumeWith(body -> {
                    Mono.justOrEmpty(body.getResponseBody())
                            .defaultIfEmpty("EMPTY")
                            .map(String::toUpperCase)
                            .block();
                });
    }

    @Test
    @DisplayName("Should return 204 No Content with security headers on POST /test")
    void testPostShouldReturnNoContent() {
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
                .expectHeader().valueEquals("Referrer-Policy", "strict-origin-when-cross-origin")
                .expectBody(String.class)
                .consumeWith(body -> {
                    Mono.justOrEmpty(body.getResponseBody())
                            .defaultIfEmpty("EMPTY")
                            .map(String::length)
                            .block();
                });
    }

    @Configuration
    static class TestRouter {
        @Bean
        public RouterFunction<ServerResponse> testRoute() {
            return route()
                    .GET("/test", req ->
                            Mono.just("ok")
                                    .flatMap(val -> ServerResponse.ok().bodyValue(val))
                    )
                    .POST("/test", req ->
                            Mono.just(req)
                                    .flatMap(r -> ServerResponse.noContent().build())
                    )
                    .build();
        }
    }

}