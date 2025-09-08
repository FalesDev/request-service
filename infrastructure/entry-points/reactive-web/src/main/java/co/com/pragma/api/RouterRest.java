package co.com.pragma.api;

import co.com.pragma.api.dto.ApplicationDto;
import co.com.pragma.api.dto.request.RegisterApplicationRequestDto;
import co.com.pragma.api.exception.GlobalExceptionHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/v1/requests",
                    method = RequestMethod.POST,
                    beanClass = Handler.class,
                    beanMethod = "registerRequest",
                    operation = @Operation(
                            operationId = "registerRequest",
                            summary = "Register a new request",
                            tags = {"Request"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(
                                            schema = @Schema(implementation = RegisterApplicationRequestDto.class)
                                    )
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "201",
                                            description = "User successfully registered",
                                            content = @Content(
                                                    mediaType = "application/json",
                                                    schema = @Schema(implementation = ApplicationDto.class)
                                            )
                                    )
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/requests",
                    method = RequestMethod.GET,
                    beanClass = Handler.class,
                    beanMethod = "getApplicationsForAdvisor",
                    operation = @Operation(
                            operationId = "getApplicationsForAdvisor",
                            summary = "Get applications for advisor",
                            tags = {"Request"},
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "List of applications retrieved successfully",
                                            content = @Content(
                                                    mediaType = "application/json",
                                                    schema = @Schema(implementation = ApplicationDto.class)
                                            )
                                    )
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> routerFunction(Handler handler,
                                                         GlobalExceptionHandler globalExceptionHandler) {
        return RouterFunctions.route()
                .POST("/api/v1/requests", handler::registerRequest)
                .GET("/api/v1/requests", handler::getApplicationsForAdvisor)
                .filter(globalExceptionHandler)
                .build();
    }
}
