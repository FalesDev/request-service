package co.com.pragma.config;

import co.com.pragma.model.application.gateways.ApplicationRepository;
import co.com.pragma.model.auth.gateway.AuthValidationGateway;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.gateways.TransactionManager;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.model.status.gateways.StatusRepository;
import co.com.pragma.usecase.getapplicationsforadvisor.GetApplicationsForAdvisorUseCase;
import co.com.pragma.usecase.registerrequest.RegisterRequestUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(basePackages = "co.com.pragma.usecase",
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$")
        },
        useDefaultFilters = false)
public class UseCasesConfig {

    @Bean
    GetApplicationsForAdvisorUseCase getApplicationsForAdvisorUseCase(
            ApplicationRepository applicationRepository,
            StatusRepository statusRepository,
            LoanTypeRepository loanTypeRepository,
            AuthValidationGateway  authValidationGateway,
            CustomLogger customLogger
    ) {
        return new GetApplicationsForAdvisorUseCase(applicationRepository, statusRepository, loanTypeRepository,
                authValidationGateway, customLogger);
    }

    @Bean
    RegisterRequestUseCase registerRequestUseCase(
            ApplicationRepository applicationRepository,
            LoanTypeRepository loanTypeRepository,
            StatusRepository statusRepository,
            TransactionManager transactionManager,
            AuthValidationGateway  authValidationGateway,
            CustomLogger customLogger
    ) {
        return new RegisterRequestUseCase(applicationRepository, loanTypeRepository, statusRepository,
                transactionManager, authValidationGateway, customLogger);
    }
}
