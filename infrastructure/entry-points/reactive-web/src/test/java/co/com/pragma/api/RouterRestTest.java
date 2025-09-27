package co.com.pragma.api;

import co.com.pragma.api.dto.ApplicationDto;
import co.com.pragma.api.dto.request.RegisterApplicationRequestDto;
import co.com.pragma.api.dto.request.UpdateApplicationStatusRequest;
import co.com.pragma.api.exception.GlobalExceptionHandler;
import co.com.pragma.api.mapper.ApplicationMapper;
import co.com.pragma.api.service.ValidationService;
import co.com.pragma.model.application.Application;
import co.com.pragma.model.application.ApplicationAdvisorView;
import co.com.pragma.model.auth.ValidatedUser;
import co.com.pragma.model.exception.UnauthorizedException;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.gateways.TokenValidator;
import co.com.pragma.model.pagination.CustomPage;
import co.com.pragma.model.pagination.CustomPageable;
import co.com.pragma.model.report.DailyReport;
import co.com.pragma.usecase.findapprovedapplicationdaily.FindApprovedApplicationDailyUseCase;
import co.com.pragma.usecase.getapplicationsforadvisor.GetApplicationsForAdvisorUseCase;
import co.com.pragma.usecase.registerrequest.RegisterRequestUseCase;
import co.com.pragma.usecase.updateapplicationstatus.UpdateApplicationStatusUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;

@WebFluxTest
@ContextConfiguration(classes = {
        RouterRest.class,
        Handler.class,
        GlobalExceptionHandler.class
})
class RouterRestTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ApplicationContext context;

    @MockitoBean
    private RegisterRequestUseCase registerRequestUseCase;

    @MockitoBean
    private GetApplicationsForAdvisorUseCase getApplicationsForAdvisorUseCase;

    @MockitoBean
    private UpdateApplicationStatusUseCase updateApplicationStatusUseCase;

    @MockitoBean
    private FindApprovedApplicationDailyUseCase findApprovedApplicationDailyUseCase;

    @MockitoBean
    private ApplicationMapper applicationMapper;

    @MockitoBean
    private ValidationService validationService;

    @MockitoBean
    private TokenValidator tokenValidator;

    @MockitoBean
    private CustomLogger customLogger;

    private RegisterApplicationRequestDto registerApplicationRequestDto;
    private Application applicationEntity;
    private ApplicationDto applicationDto;
    private ValidatedUser validatedUser;
    private UpdateApplicationStatusRequest updateApplicationStatusRequest;

    @BeforeEach
    void setUp() {
        validatedUser = ValidatedUser.builder()
                .idUser(UUID.randomUUID())
                .email("fabricio@test.com")
                .idDocument("77777777")
                .role("CLIENT")
                .build();

        applicationEntity = Application.builder()
                .id(UUID.randomUUID())
                .amount(10000.0)
                .term(12)
                .email(validatedUser.getEmail())
                .idDocument(validatedUser.getIdDocument())
                .idStatus(UUID.randomUUID())
                .idLoanType(UUID.randomUUID())
                .idUser(validatedUser.getIdUser())
                .build();

        registerApplicationRequestDto = new RegisterApplicationRequestDto(
                applicationEntity.getAmount(),
                applicationEntity.getTerm(),
                applicationEntity.getIdDocument(),
                applicationEntity.getIdLoanType()
        );

        applicationDto = new ApplicationDto(
                applicationEntity.getId(),
                applicationEntity.getAmount(),
                applicationEntity.getTerm(),
                applicationEntity.getEmail(),
                applicationEntity.getIdDocument(),
                applicationEntity.getIdStatus(),
                applicationEntity.getIdLoanType(),
                applicationEntity.getIdUser()
        );

        updateApplicationStatusRequest = new UpdateApplicationStatusRequest(
                applicationEntity.getId(),
                "APPROVED"
        );

        Mockito.when(tokenValidator.validateToken(anyString()))
                .thenReturn(Mono.just(validatedUser));

        Mockito.when(validationService.validate(any(RegisterApplicationRequestDto.class)))
                .thenReturn(Mono.just(registerApplicationRequestDto));

        Mockito.when(applicationMapper.toEntity(any(RegisterApplicationRequestDto.class)))
                .thenReturn(applicationEntity);
        Mockito.when(applicationMapper.toResponse(any(Application.class)))
                .thenReturn(applicationDto);

        Mockito.when(registerRequestUseCase.registerApplication(any(Application.class), anyString()))
                .thenReturn(Mono.just(applicationEntity));

        CustomPage<ApplicationAdvisorView> applicationsPage = CustomPage.<ApplicationAdvisorView>builder()
                .content(List.of())
                .currentPage(0)
                .totalPages(1)
                .totalElements(0L)
                .pageSize(10)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        Mockito.when(getApplicationsForAdvisorUseCase.getApplicationsByStatus(
                        anyString(),
                        eq(List.of("Pending Review", "Rejected", "Manual Review")),
                        any(CustomPageable.class)))
                .thenReturn(Mono.just(applicationsPage));

        Mockito.when(updateApplicationStatusUseCase.updateStatus(
                        any(UUID.class),
                        anyString()))
                .thenReturn(Mono.just(applicationEntity));

        DailyReport dailyReport = new DailyReport(3L, 5000.0);
        Mockito.when(findApprovedApplicationDailyUseCase.findApprovedApplicationDaily())
                .thenReturn(Mono.just(dailyReport));

        RouterRest routerRest = context.getBean(RouterRest.class);
        Handler handler = context.getBean(Handler.class);
        GlobalExceptionHandler globalExceptionHandler = context.getBean(GlobalExceptionHandler.class);

        webTestClient = WebTestClient.bindToRouterFunction(routerRest.routerFunction(handler, globalExceptionHandler))
                .build();
    }

    @Test
    @DisplayName("POST /request/api/v1/requests should return 201 Created when request is successful")
    void testRegisterRequestEndpointSuccess() {
        webTestClient.post()
                .uri("/request/api/v1/requests")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerApplicationRequestDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ApplicationDto.class)
                .value(response -> {
                    assert response.email().equals(applicationDto.email());
                    assert response.amount().equals(applicationDto.amount());
                });
    }

    @Test
    @DisplayName("POST /request/api/v1/requests should return 500 when unexpected error occurs")
    void testRegisterRequestUnexpectedException() {
        Mockito.when(validationService.validate(any(RegisterApplicationRequestDto.class)))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        webTestClient.post()
                .uri("/request/api/v1/requests")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerApplicationRequestDto)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("POST /request/api/v1/requests should return 401 when token is invalid")
    void testRegisterRequestWithInvalidToken() {
        Mockito.when(tokenValidator.validateToken(anyString()))
                .thenReturn(Mono.error(new UnauthorizedException("Invalid token")));

        webTestClient.post()
                .uri("/request/api/v1/requests")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerApplicationRequestDto)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /request/api/v1/requests should return 200 for advisor with pagination")
    void testGetApplicationsForAdvisor() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/request/api/v1/requests")
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .queryParam("sortBy", "amount")
                        .queryParam("sortDirection", "asc")
                        .build())
                .header("Authorization", "Bearer valid-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.currentPage").isEqualTo(0)
                .jsonPath("$.pageSize").isEqualTo(10);
    }

    @Test
    @DisplayName("GET /request/api/v1/requests should use default values when query parameters are missing")
    void testGetApplicationsForAdvisorWithDefaultParameters() {
        webTestClient.get()
                .uri("/request/api/v1/requests")
                .header("Authorization", "Bearer valid-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.currentPage").isEqualTo(0)
                .jsonPath("$.pageSize").isEqualTo(10)
                .jsonPath("$.totalElements").isEqualTo(0)
                .jsonPath("$.totalPages").isEqualTo(1)
                .jsonPath("$.hasNext").isEqualTo(false)
                .jsonPath("$.hasPrevious").isEqualTo(false)
                .jsonPath("$.content").isArray();
    }

    @Test
    @DisplayName("GET /request/api/v1/requests should return 401 when token is invalid")
    void testGetApplicationsForAdvisorWithInvalidToken() {
        Mockito.when(getApplicationsForAdvisorUseCase.getApplicationsByStatus(
                        anyString(),
                        any(List.class),
                        any(CustomPageable.class)))
                .thenReturn(Mono.error(new UnauthorizedException("Invalid token")));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/request/api/v1/requests")
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .build())
                .header("Authorization", "Bearer invalid-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /request/api/v1/requests should return 500 when use case fails")
    void testGetApplicationsForAdvisorWithServerError() {
        Mockito.when(getApplicationsForAdvisorUseCase.getApplicationsByStatus(
                        anyString(),
                        any(List.class),
                        any(CustomPageable.class)))
                .thenReturn(Mono.error(new RuntimeException("Server error")));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/request/api/v1/requests")
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .build())
                .header("Authorization", "Bearer valid-token")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("POST /request/api/v1/requests should return 400 when authorization header is missing")
    void testRegisterRequestWithoutAuthorizationHeader() {
        webTestClient.post()
                .uri("/request/api/v1/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerApplicationRequestDto)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /request/api/v1/requests should return 401 when authorization header is missing")
    void testGetApplicationsForAdvisorWithoutAuthorizationHeader() {
        webTestClient.get()
                .uri("/request/api/v1/requests")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("PUT /request/api/v1/requests should return 200 when update is successful")
    void testUpdateApplicationStatusSuccess() {
        webTestClient.put()
                .uri("/request/api/v1/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateApplicationStatusRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ApplicationDto.class)
                .value(response -> {
                    assert response.id().equals(applicationDto.id());
                    assert response.idStatus().equals(applicationDto.idStatus());
                });
    }

    @Test
    @DisplayName("PUT /request/api/v1/requests should return 500 when update fails")
    void testUpdateApplicationStatusWithServerError() {
        Mockito.when(updateApplicationStatusUseCase.updateStatus(any(UUID.class), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Update error")));

        webTestClient.put()
                .uri("/request/api/v1/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateApplicationStatusRequest)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("GET /request/api/v1/requests/approved/yesterday should return 200 with daily report")
    void testGetApprovedApplicationDailySuccess() {
        DailyReport dailyReport = new DailyReport(3L, 5000.0);
        Mockito.when(findApprovedApplicationDailyUseCase.findApprovedApplicationDaily())
                .thenReturn(Mono.just(dailyReport));

        webTestClient.get()
                .uri("/request/api/v1/requests/approved/yesterday")
                .header("Authorization", "Bearer valid-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody(DailyReport.class)
                .value(response -> {
                    assert response.getApprovedLoansCount().equals(dailyReport.getApprovedLoansCount());
                    assert response.getTotalLoanAmount().equals(dailyReport.getTotalLoanAmount());
                });
    }

    @Test
    @DisplayName("GET /request/api/v1/requests/approved/yesterday should return 500 when use case fails")
    void testGetApprovedApplicationDailyWithServerError() {
        Mockito.when(findApprovedApplicationDailyUseCase.findApprovedApplicationDaily())
                .thenReturn(Mono.error(new RuntimeException("Server error")));

        webTestClient.get()
                .uri("/request/api/v1/requests/approved/yesterday")
                .header("Authorization", "Bearer valid-token")
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
