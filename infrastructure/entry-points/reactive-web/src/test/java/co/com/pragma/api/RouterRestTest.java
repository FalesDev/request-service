package co.com.pragma.api;

import co.com.pragma.api.dto.ApplicationDto;
import co.com.pragma.api.dto.request.RegisterApplicationRequestDto;
import co.com.pragma.api.exception.GlobalExceptionHandler;
import co.com.pragma.api.mapper.ApplicationMapper;
import co.com.pragma.api.service.ValidationService;
import co.com.pragma.model.application.Application;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.usecase.registerrequest.RegisterRequestUseCase;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;

@ContextConfiguration(classes = {
        RouterRest.class,
        Handler.class,
        GlobalExceptionHandler.class
})
@WebFluxTest
class RouterRestTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private RegisterRequestUseCase registerRequestUseCase;
    @MockitoBean private ApplicationMapper applicationMapper;
    @MockitoBean private ValidationService validationService;
    @MockitoBean private CustomLogger customLogger;

    private RegisterApplicationRequestDto registerApplicationRequestDto;

    @BeforeEach
    void setUp() {
        Application applicationEntity = new Application();
        applicationEntity.setId(UUID.randomUUID());
        applicationEntity.setAmount(10000.0);
        applicationEntity.setTerm(12);
        applicationEntity.setEmail("fabricio@test.com");
        applicationEntity.setIdDocument("77777777");
        applicationEntity.setIdStatus(UUID.randomUUID());
        applicationEntity.setIdLoanType(UUID.randomUUID());

        registerApplicationRequestDto = new RegisterApplicationRequestDto(
                applicationEntity.getAmount(),
                applicationEntity.getTerm(),
                applicationEntity.getEmail(),
                applicationEntity.getIdDocument(),
                applicationEntity.getIdLoanType()
        );

        ApplicationDto applicationDto = new ApplicationDto(
                applicationEntity.getId(),
                applicationEntity.getAmount(),
                applicationEntity.getTerm(),
                applicationEntity.getEmail(),
                applicationEntity.getIdDocument(),
                applicationEntity.getIdStatus(),
                applicationEntity.getIdLoanType()
        );

        Mockito.when(validationService.validate(any(RegisterApplicationRequestDto.class)))
                .thenReturn(Mono.just(registerApplicationRequestDto));

        Mockito.when(applicationMapper.toEntity(any(RegisterApplicationRequestDto.class)))
                .thenReturn(applicationEntity);
        Mockito.when(applicationMapper.toResponse(any(Application.class)))
                .thenReturn(applicationDto);

        Mockito.when(registerRequestUseCase.registerApplication(any(Application.class)))
                .thenReturn(Mono.just(applicationEntity));

        Handler handler = new Handler(registerRequestUseCase, applicationMapper, validationService);
        webTestClient = WebTestClient.bindToRouterFunction(
                new RouterRest().routerFunction(handler, new GlobalExceptionHandler(customLogger))
        ).build();
    }

    @Test
    @DisplayName("Should return 201 Created when register-request request is successful")
    void testRegisterRequestEndpointSuccess() {
        webTestClient.post()
                .uri("/api/v1/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerApplicationRequestDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ApplicationDto.class)
                .value(response -> {
                    Assertions.assertThat(response.email()).isEqualTo(registerApplicationRequestDto.email());
                    Assertions.assertThat(response.amount()).isEqualTo(registerApplicationRequestDto.amount());
                });
    }

    @Test
    @DisplayName("Should return 500 when unexpected error occurs in register-request")
    void testRegisterRequestUnexpectedException() {
        Mockito.when(validationService.validate(any(RegisterApplicationRequestDto.class)))
                .thenReturn(Mono.error(new RuntimeException()));

        webTestClient.post()
                .uri("/api/v1/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerApplicationRequestDto)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.status").isEqualTo("500");
    }
}
