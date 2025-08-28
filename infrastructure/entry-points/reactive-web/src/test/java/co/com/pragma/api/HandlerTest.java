package co.com.pragma.api;

import co.com.pragma.api.dto.ApplicationDto;
import co.com.pragma.api.dto.request.RegisterApplicationRequestDto;
import co.com.pragma.api.mapper.ApplicationMapper;
import co.com.pragma.api.service.ValidationService;
import co.com.pragma.model.application.Application;
import co.com.pragma.usecase.registerrequest.RegisterRequestUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HandlerTest {

    @Mock
    private RegisterRequestUseCase registerRequestUseCase;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private ValidationService validationService;

    @Mock
    private ServerRequest request;

    @InjectMocks
    private Handler handler;

    private RegisterApplicationRequestDto requestDto;
    private Application application;
    private ApplicationDto responseDto;

    @BeforeEach
    void setup() {
        requestDto = new RegisterApplicationRequestDto(
                20000.0,
                12,
                "fabricio@example.com",
                "77777777",
                UUID.randomUUID()
        );

        application = Application.builder()
                .id(UUID.randomUUID())
                .amount(requestDto.amount())
                .term(requestDto.term())
                .email(requestDto.email())
                .idDocument(requestDto.idDocument())
                .idStatus(UUID.randomUUID())
                .idLoanType(UUID.randomUUID())
                .build();

        responseDto = new ApplicationDto(
                application.getId(),
                20000.0,
                12,
                "fabricio@example.com",
                "77777777",
                application.getIdStatus(),
                application.getIdLoanType()
        );
    }

    @Test
    @DisplayName("Should register application and return 201")
    void testRegisterRequestSuccess() {
        when(request.bodyToMono(RegisterApplicationRequestDto.class)).thenReturn(Mono.just(requestDto));
        when(validationService.validate(requestDto)).thenReturn(Mono.just(requestDto));
        when(applicationMapper.toEntity(requestDto)).thenReturn(application);
        when(registerRequestUseCase.registerApplication(application)).thenReturn(Mono.just(application));
        when(applicationMapper.toResponse(application)).thenReturn(responseDto);

        Mono<ServerResponse> responseMono = handler.registerRequest(request);

        StepVerifier.create(responseMono)
                .expectNextCount(1)
                .verifyComplete();
    }
}
