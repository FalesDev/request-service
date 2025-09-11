package co.com.pragma.config;

import co.com.pragma.model.application.gateways.ApplicationRepository;
import co.com.pragma.model.auth.gateway.AuthValidationGateway;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.gateways.NotificationGateway;
import co.com.pragma.model.gateways.TransactionManager;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.model.status.gateways.StatusRepository;
import co.com.pragma.usecase.findloantypebyid.FindLoanTypeByIdUseCase;
import co.com.pragma.usecase.getapplicationsforadvisor.GetApplicationsForAdvisorUseCase;
import co.com.pragma.usecase.registerrequest.RegisterRequestUseCase;
import co.com.pragma.usecase.updateapplicationstatus.UpdateApplicationStatusUseCase;
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
    @DisplayName("Should register FindLoanTypeByIdUseCase bean in application context")
    void testFindLoanTypeByIdUseCaseBeanExists() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(TestConfig.class)) {

            FindLoanTypeByIdUseCase findLoanTypeByIdUseCase = context.getBean(FindLoanTypeByIdUseCase.class);
            assertNotNull(findLoanTypeByIdUseCase, "FindLoanTypeByIdUseCase bean should be registered");
        }
    }

    @Configuration
    @Import(UseCasesConfig.class)
    static class TestConfig {
        @Bean
        ApplicationRepository userRepository() { return mock(ApplicationRepository.class); }
        @Bean
        LoanTypeRepository loanTypeRepository() { return mock(LoanTypeRepository.class); }
        @Bean
        StatusRepository statusRepository() { return mock(StatusRepository.class); }
        @Bean
        TransactionManager transactionManager() { return mock(TransactionManager.class); }
        @Bean
        AuthValidationGateway authValidationGateway() { return mock(AuthValidationGateway.class); }
        @Bean
        CustomLogger customLogger() { return mock(CustomLogger.class); }

        @Bean
        NotificationGateway notificationGateway() { return mock(NotificationGateway.class); }
    }
}