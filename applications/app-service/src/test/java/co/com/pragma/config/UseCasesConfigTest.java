package co.com.pragma.config;

import co.com.pragma.model.application.gateways.ApplicationRepository;
import co.com.pragma.model.auth.gateway.AuthValidationGateway;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.gateways.TransactionManager;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.model.status.gateways.StatusRepository;
import co.com.pragma.usecase.getapplicationsforadvisor.GetApplicationsForAdvisorUseCase;
import co.com.pragma.usecase.registerrequest.RegisterRequestUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

public class UseCasesConfigTest {

    @Test
    @DisplayName("Should register GetApplicationsForAdvisorUseCase bean in application context")
    void testGetApplicationsForAdvisorUseCaseBeanExists() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(TestConfig.class)) {

            GetApplicationsForAdvisorUseCase getApplicationsForAdvisorUseCase = context.getBean(GetApplicationsForAdvisorUseCase.class);
            assertNotNull(getApplicationsForAdvisorUseCase, "GetApplicationsForAdvisorUseCase bean should be registered");
        }
    }

    @Test
    @DisplayName("Should register RegisterRequestUseCase bean in application context")
    void testRegisterRequestUseCaseBeanExists() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(TestConfig.class)) {

            RegisterRequestUseCase registerRequestUseCase = context.getBean(RegisterRequestUseCase.class);
            assertNotNull(registerRequestUseCase, "RegisterRequestUseCase bean should be registered");
        }
    }

    @Configuration
    @Import(UseCasesConfig.class)
    static class TestConfig {
        @Bean
        ApplicationRepository userRepository() { return mock(ApplicationRepository.class); }
        @Bean
        LoanTypeRepository roleRepository() { return mock(LoanTypeRepository.class); }
        @Bean
        StatusRepository passwordEncoder() { return mock(StatusRepository.class); }
        @Bean
        TransactionManager transactionManager() { return mock(TransactionManager.class); }
        @Bean
        AuthValidationGateway authValidationGateway() { return mock(AuthValidationGateway.class); }
        @Bean
        CustomLogger customLogger() { return mock(CustomLogger.class); }
    }
}