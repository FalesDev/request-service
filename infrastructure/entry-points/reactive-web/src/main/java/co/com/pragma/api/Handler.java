package co.com.pragma.api;

import co.com.pragma.api.dto.request.RegisterApplicationRequestDto;
import co.com.pragma.api.mapper.ApplicationMapper;
import co.com.pragma.api.service.ValidationService;
import co.com.pragma.model.auth.ValidatedUser;
import co.com.pragma.model.exception.UnauthorizedException;
import co.com.pragma.model.gateways.TokenValidator;
import co.com.pragma.model.pagination.CustomPageable;
import co.com.pragma.usecase.getapplicationsforadvisor.GetApplicationsForAdvisorUseCase;
import co.com.pragma.usecase.registerrequest.RegisterRequestUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class Handler {
    private final RegisterRequestUseCase registerRequestUseCase;
    private final GetApplicationsForAdvisorUseCase getApplicationsForAdvisorUseCase;
    private final ApplicationMapper applicationMapper;
    private final ValidationService validationService;
    private final TokenValidator tokenValidator;

    public Mono<ServerResponse> registerRequest(ServerRequest request) {
        return extractAuthToken(request)
                .flatMap(token ->
                        tokenValidator.validateToken(token)
                                .zipWith(request.bodyToMono(RegisterApplicationRequestDto.class))
                                .flatMap(tuple -> {
                                    ValidatedUser validatedUser = tuple.getT1();
                                    RegisterApplicationRequestDto requestDto = tuple.getT2();

                                    if (!validatedUser.getIdDocument().equals(requestDto.idDocument())) {
                                        return Mono.error(new UnauthorizedException(
                                                "Clients can only create loan requests for themselves"));
                                    }

                                    return validationService.validate(requestDto)
                                            .map(applicationMapper::toEntity)
                                            .flatMap(application ->
                                                    registerRequestUseCase.registerApplication(application, token)
                                            )
                                            .map(applicationMapper::toResponse);
                                })
                )
                .flatMap(dto -> ServerResponse
                        .status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto)
                );
    }

    public Mono<ServerResponse> getApplicationsForAdvisor(ServerRequest request) {
        return extractAuthToken(request)
                .flatMap(token -> {
                    int page = Integer.parseInt(request.queryParam("page").orElse("0"));
                    int size = Integer.parseInt(request.queryParam("size").orElse("10"));
                    String sortBy = request.queryParam("sortBy").orElse("amount");
                    String sortDirection = request.queryParam("sortDirection").orElse("asc");

                    CustomPageable customPageable = CustomPageable.builder()
                            .page(page)
                            .size(size)
                            .sortBy(sortBy)
                            .sortDirection(sortDirection)
                            .build();

                    List<String> targetStatuses = List.of("Pending Review", "Rejected", "Manual Review");

                    return getApplicationsForAdvisorUseCase.getApplicationsByStatus(token, targetStatuses, customPageable);
                })
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response));
    }

    private Mono<String> extractAuthToken(ServerRequest request) {
        return Mono.justOrEmpty(request.headers().firstHeader(HttpHeaders.AUTHORIZATION))
                .filter(token -> token.startsWith("Bearer "))
                .map(token -> token.substring(7))
                .switchIfEmpty(Mono.error(new UnauthorizedException("Authorization header is missing or invalid")));
    }
}
