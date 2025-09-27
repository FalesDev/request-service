package co.com.pragma.security;

import co.com.pragma.model.auth.ValidatedUser;
import co.com.pragma.model.exception.TokenValidationException;
import co.com.pragma.model.gateways.TokenValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.UUID;

@Component
public class JwtValidatorAdapter implements TokenValidator {

    private final SecretKey secretKey;

    public JwtValidatorAdapter(@Value("${jwt.secret}") String secretKeyString) {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKeyString));
    }

    @Override
    public Mono<ValidatedUser> validateToken(String token) {
        return Mono.fromSupplier(() -> {
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                return ValidatedUser.builder()
                        .idUser(UUID.fromString(claims.get("idUser", String.class)))
                        .email(claims.getSubject())
                        .idDocument(claims.get("idDocument", String.class))
                        .role(claims.get("role", String.class))
                        .build();
            } catch (SignatureException ex) {
                throw new TokenValidationException("Invalid JWT signature");
            } catch (ExpiredJwtException ex) {
                throw new TokenValidationException("JWT token expired");
            } catch (JwtException ex) {
                throw new TokenValidationException("Invalid JWT token: " + ex.getMessage());
            }
        });
    }
}
