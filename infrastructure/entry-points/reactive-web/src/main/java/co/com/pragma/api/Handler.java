package co.com.pragma.api;

import co.com.pragma.api.dto.request.RegisterApplicationRequestDto;
import co.com.pragma.api.mapper.ApplicationMapper;
import co.com.pragma.api.service.ValidationService;
import co.com.pragma.usecase.registerrequest.RegisterRequestUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class Handler {

    private final RegisterRequestUseCase registerRequestUseCase;
    private final ApplicationMapper applicationMapper;
    private final ValidationService validationService;

    public Mono<ServerResponse> registerRequest(ServerRequest request) {
        return request.bodyToMono(RegisterApplicationRequestDto.class)
                .flatMap(validationService::validate)
                .map(applicationMapper::toEntity)
                .flatMap(registerRequestUseCase::registerApplication)
                .map(applicationMapper::toResponse)
                .flatMap(dto -> ServerResponse
                        .status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(dto)
                );
    }
}
