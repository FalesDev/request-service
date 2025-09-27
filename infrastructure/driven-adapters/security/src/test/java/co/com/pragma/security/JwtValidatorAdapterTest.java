package co.com.pragma.security;

import co.com.pragma.model.exception.TokenValidationException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class JwtValidatorAdapterTest {

    private JwtValidatorAdapter jwtValidatorAdapter;
    private SecretKey testSecretKey;

    @BeforeEach
    void setUp() {
        String testSecret = "testSecretKeyWhichIsLongEnoughForHS256Algorithm";
        String base64Secret = Base64.getEncoder().encodeToString(testSecret.getBytes());
        jwtValidatorAdapter = new JwtValidatorAdapter(base64Secret);
        testSecretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret));
    }

    @Test
    @DisplayName("Should validate token successfully and return ValidatedUser")
    void validateToken_Success() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String idDocument = "12345678";
        String role = "USER";

        String token = Jwts.builder()
                .claim("idUser", userId.toString())
                .subject(email)
                .claim("idDocument", idDocument)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour
                .signWith(testSecretKey)
                .compact();

        StepVerifier.create(jwtValidatorAdapter.validateToken(token))
                .expectNextMatches(validatedUser ->
                        validatedUser.getIdUser().equals(userId) &&
                                validatedUser.getEmail().equals(email) &&
                                validatedUser.getIdDocument().equals(idDocument) &&
                                validatedUser.getRole().equals(role))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw TokenValidationException for invalid signature")
    void validateToken_InvalidSignature() {
        SecretKey wrongSecretKey = Keys.hmacShaKeyFor("differentSecretKeyWhichIsAlsoLongEnough".getBytes());

        String token = Jwts.builder()
                .claim("idUser", UUID.randomUUID().toString())
                .subject("test@example.com")
                .claim("idDocument", "12345678")
                .claim("role", "USER")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(wrongSecretKey)
                .compact();

        StepVerifier.create(jwtValidatorAdapter.validateToken(token))
                .expectErrorMatches(throwable ->
                        throwable instanceof TokenValidationException &&
                                throwable.getMessage().equals("Invalid JWT signature"))
                .verify();
    }

    @Test
    @DisplayName("Should throw TokenValidationException for expired token")
    void validateToken_ExpiredToken() {
        String token = Jwts.builder()
                .claim("idUser", UUID.randomUUID().toString())
                .subject("test@example.com")
                .claim("idDocument", "12345678")
                .claim("role", "USER")
                .issuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 2))
                .expiration(new Date(System.currentTimeMillis() - 1000 * 60 * 60))
                .signWith(testSecretKey)
                .compact();

        StepVerifier.create(jwtValidatorAdapter.validateToken(token))
                .expectErrorMatches(throwable ->
                        throwable instanceof TokenValidationException &&
                                throwable.getMessage().equals("JWT token expired"))
                .verify();
    }

    @Test
    @DisplayName("Should throw TokenValidationException for malformed token")
    void validateToken_MalformedToken() {
        String malformedToken = "malformed.token.here";

        StepVerifier.create(jwtValidatorAdapter.validateToken(malformedToken))
                .expectErrorMatches(throwable ->
                        throwable instanceof TokenValidationException &&
                                throwable.getMessage().startsWith("Invalid JWT token:"))
                .verify();
    }
}