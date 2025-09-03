package co.com.pragma.api.security;

import co.com.pragma.model.exception.TokenValidationException;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.gateways.TokenValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private final TokenValidator tokenValidator;
    private final CustomLogger logger;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = extractToken(exchange.getRequest());
        if (token == null) {
            return chain.filter(exchange);
        }

        return tokenValidator.validateToken(token)
                .map(validatedUser -> {
                    String role = validatedUser.getRole();
                    String username = validatedUser.getEmail();
                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    return new UsernamePasswordAuthenticationToken(username, null, authorities);
                })
                .flatMap(authentication -> {
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                })
                .onErrorResume(e -> {
                    if (e instanceof TokenValidationException) {
                        logger.warn("Token validation failed: {}", e.getMessage());
                        exchange.getAttributes().put("AUTH_ERROR", e.getMessage());
                    }
                    return chain.filter(exchange);
                });
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
