package co.com.pragma.api;

import co.com.pragma.api.dto.ApplicationDto;
import co.com.pragma.api.dto.request.RegisterApplicationRequestDto;
import co.com.pragma.api.dto.request.UpdateApplicationStatusRequest;
import co.com.pragma.api.exception.GlobalExceptionHandler;
import co.com.pragma.model.report.DailyReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@RequiredArgsConstructor
public class RouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/request/api/v1/requests",
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
                    path = "/request/api/v1/requests",
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
            ),
            @RouterOperation(
                    path = "/request/api/v1/requests",
                    method = RequestMethod.PUT,
                    beanClass = Handler.class,
                    beanMethod = "updateApplicationStatus",
                    operation = @Operation(
                            operationId = "updateApplicationStatus",
                            summary = "Update Application Status",
                            tags = {"Request"},
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(
                                            schema = @Schema(implementation = UpdateApplicationStatusRequest.class)
                                    )
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Application updated",
                                            content = @Content(
                                                    mediaType = "application/json",
                                                    schema = @Schema(implementation = ApplicationDto.class)
                                            )
                                    )
                            }
                    )
            ),
            @RouterOperation(
                    path = "/request/api/v1/requests/approved/yesterday",
                    method = RequestMethod.GET,
                    beanClass = Handler.class,
                    beanMethod = "getApprovedApplicationDaily",
                    operation = @Operation(
                            operationId = "getApprovedApplicationDaily",
                            summary = "Get approved applications daily",
                            tags = {"Request"},
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "List of applications retrieved successfully",
                                            content = @Content(
                                                    mediaType = "application/json",
                                                    schema = @Schema(implementation = DailyReport.class)
                                            )
                                    )
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> routerFunction(Handler handler,
                                                         GlobalExceptionHandler globalExceptionHandler) {
        return RouterFunctions.route()
                .POST("/request/api/v1/requests", handler::registerRequest)
                .GET("/request/api/v1/requests", handler::getApplicationsForAdvisor)
                .PUT("/request/api/v1/requests", handler::updateApplicationStatus)
                .GET("/request/api/v1/requests/approved/yesterday", handler::getApprovedApplicationDaily)
                .filter(globalExceptionHandler)
                .build();
    }
}
