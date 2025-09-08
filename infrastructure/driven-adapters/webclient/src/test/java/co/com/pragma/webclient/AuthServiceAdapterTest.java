package co.com.pragma.webclient;

import co.com.pragma.model.auth.UserFound;
import co.com.pragma.model.auth.ValidatedUser;
import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.exception.UnauthorizedException;
import co.com.pragma.webclient.dto.UserValidationRequest;
import co.com.pragma.webclient.dto.UsersFoundRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class AuthServiceAdapterTest {

    private MockWebServer mockWebServer;
    private AuthServiceAdapter authServiceAdapter;
    private ObjectMapper objectMapper;
    private final String token = "test-token";

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        authServiceAdapter = new AuthServiceAdapter(webClient);
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("validateClientUser should return validated user when response is successful")
    void validateClientUser_Success() throws Exception {
        String idDocument = "12345678";
        ValidatedUser expectedUser = ValidatedUser.builder()
                .idUser(UUID.randomUUID())
                .email("test@example.com")
                .idDocument(idDocument)
                .role("USER")
                .build();

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(expectedUser)));

        StepVerifier.create(authServiceAdapter.validateClientUser(idDocument, token))
                .expectNextMatches(validatedUser ->
                        validatedUser.getIdUser().equals(expectedUser.getIdUser()) &&
                                validatedUser.getEmail().equals(expectedUser.getEmail()) &&
                                validatedUser.getIdDocument().equals(expectedUser.getIdDocument()) &&
                                validatedUser.getRole().equals(expectedUser.getRole()))
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/api/v1/users/document", recordedRequest.getPath());
        assertEquals("Bearer " + token, recordedRequest.getHeader(HttpHeaders.AUTHORIZATION));

        UserValidationRequest requestBody = objectMapper.readValue(
                recordedRequest.getBody().readUtf8(), UserValidationRequest.class);
        assertEquals(idDocument, requestBody.idDocument());
    }

    @Test
    @DisplayName("validateClientUser should throw UnauthorizedException when response is 401")
    void validateClientUser_Unauthorized() {
        String idDocument = "12345678";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.UNAUTHORIZED.value()));

        StepVerifier.create(authServiceAdapter.validateClientUser(idDocument, token))
                .expectErrorMatches(ex ->
                        ex instanceof UnauthorizedException &&
                                ex.getMessage().equals("Unauthorized: Invalid token"))
                .verify();
    }

    @Test
    @DisplayName("validateClientUser should throw EntityNotFoundException when response is 404")
    void validateClientUser_NotFound() {
        String idDocument = "12345678";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.NOT_FOUND.value()));

        StepVerifier.create(authServiceAdapter.validateClientUser(idDocument, token))
                .expectErrorMatches(ex ->
                        ex instanceof EntityNotFoundException &&
                                ex.getMessage().equals("User not found in auth service"))
                .verify();
    }

    @Test
    @DisplayName("validateClientUser should complete without error when response is other 4xx")
    void validateClientUser_Other4xxError() {
        String idDocument = "12345678";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.BAD_REQUEST.value()));

        StepVerifier.create(authServiceAdapter.validateClientUser(idDocument, token))
                .expectComplete()
                .verify();
    }

    // -------------------- foundClientByIds --------------------

    @Test
    @DisplayName("foundClientByIds should return users when response is successful")
    void foundClientByIds_Success() throws Exception {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        List<UserFound> expectedUsers = List.of(
                UserFound.builder()
                        .idUser(userId1)
                        .email("john@example.com")
                        .firstName("John")
                        .lastName("Doe")
                        .baseSalary(3000.0)
                        .build(),
                UserFound.builder()
                        .idUser(userId2)
                        .email("jane@example.com")
                        .firstName("Jane")
                        .lastName("Smith")
                        .baseSalary(4000.0)
                        .build()
        );

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(expectedUsers)));

        StepVerifier.create(authServiceAdapter.foundClientByIds(List.of(userId1, userId2), token))
                .expectNextMatches(user -> expectedUsers.stream().anyMatch(u -> u.getIdUser().equals(user.getIdUser())))
                .expectNextMatches(user -> expectedUsers.stream().anyMatch(u -> u.getIdUser().equals(user.getIdUser())))
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/api/v1/users/find", recordedRequest.getPath());
        assertEquals("Bearer " + token, recordedRequest.getHeader(HttpHeaders.AUTHORIZATION));

        UsersFoundRequest requestBody = objectMapper.readValue(
                recordedRequest.getBody().readUtf8(), UsersFoundRequest.class);
        assertEquals(2, requestBody.userIds().size());
    }

    @Test
    @DisplayName("foundClientByIds should throw UnauthorizedException when response is 401")
    void foundClientByIds_Unauthorized() {
        UUID userId = UUID.randomUUID();

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.UNAUTHORIZED.value()));

        StepVerifier.create(authServiceAdapter.foundClientByIds(List.of(userId), token))
                .expectErrorMatches(ex ->
                        ex instanceof UnauthorizedException &&
                                ex.getMessage().equals("Unauthorized: Invalid token"))
                .verify();
    }

    @Test
    @DisplayName("foundClientByIds should throw EntityNotFoundException when response is 404")
    void foundClientByIds_NotFound() {
        UUID userId = UUID.randomUUID();

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.NOT_FOUND.value()));

        StepVerifier.create(authServiceAdapter.foundClientByIds(List.of(userId), token))
                .expectErrorMatches(ex ->
                        ex instanceof EntityNotFoundException &&
                                ex.getMessage().equals("Users not found in auth service"))
                .verify();
    }

    @Test
    @DisplayName("foundClientByIds should complete without error when response is other 4xx")
    void foundClientByIds_Other4xxError() {
        UUID userId = UUID.randomUUID();

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(HttpStatus.BAD_REQUEST.value()));

        StepVerifier.create(authServiceAdapter.foundClientByIds(List.of(userId), token))
                .expectComplete()
                .verify();
    }
}