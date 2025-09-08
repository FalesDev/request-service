package co.com.pragma.api;

import co.com.pragma.api.dto.ApplicationDto;
import co.com.pragma.api.dto.request.RegisterApplicationRequestDto;
import co.com.pragma.api.mapper.ApplicationMapper;
import co.com.pragma.api.service.ValidationService;
import co.com.pragma.model.application.Application;
import co.com.pragma.model.auth.ValidatedUser;
import co.com.pragma.model.exception.UnauthorizedException;
import co.com.pragma.model.gateways.TokenValidator;
import co.com.pragma.model.pagination.CustomPage;
import co.com.pragma.model.pagination.CustomPageable;
import co.com.pragma.usecase.getapplicationsforadvisor.GetApplicationsForAdvisorUseCase;
import co.com.pragma.usecase.registerrequest.RegisterRequestUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HandlerTest {

    @Mock
    private RegisterRequestUseCase registerRequestUseCase;

    @Mock
    private GetApplicationsForAdvisorUseCase getApplicationsForAdvisorUseCase;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private ValidationService validationService;

    @Mock
    private TokenValidator tokenValidator;

    @Mock
    private ServerRequest serverRequest;

    @Mock
    private ServerRequest.Headers headers;

    @InjectMocks
    private Handler handler;

    private RegisterApplicationRequestDto requestDto;
    private Application application;
    private ApplicationDto responseDto;
    private ValidatedUser validatedUser;
    private String token;

    @BeforeEach
    void setup() {
        token = "test-token";

        requestDto = new RegisterApplicationRequestDto(
                20000.0,
                12,
                "12345678",
                UUID.randomUUID()
        );

        validatedUser = ValidatedUser.builder()
                .idUser(UUID.randomUUID())
                .email("user@test.com")
                .idDocument("12345678")
                .role("CLIENT")
                .build();

        application = Application.builder()
                .id(UUID.randomUUID())
                .amount(requestDto.amount())
                .term(requestDto.term())
                .email(validatedUser.getEmail())
                .idDocument(requestDto.idDocument())
                .idStatus(UUID.randomUUID())
                .idLoanType(requestDto.idLoanType())
                .idUser(validatedUser.getIdUser())
                .build();

        responseDto = new ApplicationDto(
                application.getId(),
                application.getAmount(),
                application.getTerm(),
                application.getEmail(),
                application.getIdDocument(),
                application.getIdStatus(),
                application.getIdLoanType(),
                application.getIdUser()
        );
    }

    @Test
    @DisplayName("Should register application successfully for client")
    void registerRequest_SuccessForClient() {
        when(serverRequest.headers()).thenReturn(headers);
        when(headers.firstHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(serverRequest.bodyToMono(RegisterApplicationRequestDto.class)).thenReturn(Mono.just(requestDto));
        when(tokenValidator.validateToken(token)).thenReturn(Mono.just(validatedUser));
        when(validationService.validate(requestDto)).thenReturn(Mono.just(requestDto));
        when(applicationMapper.toEntity(requestDto)).thenReturn(application);
        when(registerRequestUseCase.registerApplication(application, token)).thenReturn(Mono.just(application));
        when(applicationMapper.toResponse(application)).thenReturn(responseDto);

        StepVerifier.create(handler.registerRequest(serverRequest))
                .expectNextMatches(serverResponse -> serverResponse.statusCode() == HttpStatus.CREATED)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return unauthorized when client tries to create request for another user")
    void registerRequest_ClientTriesForAnotherUser() {
        RegisterApplicationRequestDto differentUserRequest = new RegisterApplicationRequestDto(
                20000.0,
                12,
                "different-document",
                UUID.randomUUID()
        );

        when(serverRequest.headers()).thenReturn(headers);
        when(headers.firstHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(serverRequest.bodyToMono(RegisterApplicationRequestDto.class)).thenReturn(Mono.just(differentUserRequest));
        when(tokenValidator.validateToken(token)).thenReturn(Mono.just(validatedUser));

        StepVerifier.create(handler.registerRequest(serverRequest))
                .expectErrorMatches(throwable -> throwable instanceof UnauthorizedException &&
                        throwable.getMessage().equals("Clients can only create loan requests for themselves"))
                .verify();
    }


    @Test
    @DisplayName("Should return unauthorized for non-advisor users")
    void getApplicationsForAdvisor_NonAdvisorUser() {
        when(serverRequest.headers()).thenReturn(headers);
        when(headers.firstHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(tokenValidator.validateToken(token)).thenReturn(Mono.just(validatedUser));

        StepVerifier.create(handler.getApplicationsForAdvisor(serverRequest))
                .expectNextMatches(serverResponse -> serverResponse.statusCode() == HttpStatus.UNAUTHORIZED)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle missing authorization header")
    void extractAuthToken_MissingHeader() {
        when(serverRequest.headers()).thenReturn(headers);
        when(headers.firstHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        StepVerifier.create(handler.registerRequest(serverRequest))
                .expectErrorMatches(throwable -> throwable instanceof UnauthorizedException &&
                        throwable.getMessage().equals("Authorization header is missing or invalid"))
                .verify();
    }

    @Test
    @DisplayName("Should handle malformed authorization header")
    void extractAuthToken_MalformedHeader() {
        when(serverRequest.headers()).thenReturn(headers);
        when(headers.firstHeader(HttpHeaders.AUTHORIZATION)).thenReturn("InvalidFormat");

        StepVerifier.create(handler.registerRequest(serverRequest))
                .expectErrorMatches(throwable -> throwable instanceof UnauthorizedException &&
                        throwable.getMessage().equals("Authorization header is missing or invalid"))
                .verify();
    }
}