package co.com.pragma.config;

import co.com.pragma.model.application.gateways.ApplicationRepository;
import co.com.pragma.model.auth.gateway.AuthValidationGateway;
import co.com.pragma.model.creditanalysis.PaymentPlanGenerator;
import co.com.pragma.model.creditanalysis.gateway.CreditAnalysisGateway;
import co.com.pragma.model.gateways.ApplicationConfigurationProvider;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.gateways.NotificationGateway;
import co.com.pragma.model.gateways.TransactionManager;
import co.com.pragma.model.report.gateways.ReportApprovedGateway;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.model.status.gateways.StatusRepository;
import co.com.pragma.usecase.findapprovedapplicationdaily.FindApprovedApplicationDailyUseCase;
import co.com.pragma.usecase.findloantypebyid.FindLoanTypeByIdUseCase;
import co.com.pragma.usecase.getapplicationsforadvisor.GetApplicationsForAdvisorUseCase;
import co.com.pragma.usecase.processapplicationdecision.ProcessApplicationDecisionUseCase;
import co.com.pragma.usecase.registerrequest.RegisterRequestUseCase;
import co.com.pragma.usecase.updateapplicationstatus.UpdateApplicationStatusUseCase;
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
    FindApprovedApplicationDailyUseCase findApprovedApplicationDailyUseCase(
            ApplicationRepository applicationRepository,
            StatusRepository statusRepository,
            ApplicationConfigurationProvider applicationConfigurationProvider,
            CustomLogger customLogger
    ) {
        return new FindApprovedApplicationDailyUseCase(applicationRepository, statusRepository,
                applicationConfigurationProvider, customLogger);
    }

    @Bean
    FindLoanTypeByIdUseCase findLoanTypeByIdUseCase(
            LoanTypeRepository loanTypeRepository,
            CustomLogger customLogger
    ) {
        return new FindLoanTypeByIdUseCase(loanTypeRepository, customLogger);
    }

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
    ProcessApplicationDecisionUseCase processApplicationDecisionUseCase(
            ApplicationRepository applicationRepository,
            StatusRepository statusRepository,
            LoanTypeRepository loanTypeRepository,
            NotificationGateway notificationGateway,
            PaymentPlanGenerator paymentPlanGenerator,
            ReportApprovedGateway reportApprovedGateway,
            CustomLogger customLogger
    ) {
        return new ProcessApplicationDecisionUseCase(applicationRepository, statusRepository,loanTypeRepository,
                notificationGateway, paymentPlanGenerator, reportApprovedGateway,customLogger);
    }

    @Bean
    RegisterRequestUseCase registerRequestUseCase(
            ApplicationRepository applicationRepository,
            StatusRepository statusRepository,
            LoanTypeRepository loanTypeRepository,
            TransactionManager transactionManager,
            AuthValidationGateway authValidationGateway,
            FindLoanTypeByIdUseCase findLoanTypeByIdUseCase,
            CreditAnalysisGateway creditAnalysisGateway,
            CustomLogger customLogger
    ) {
        return new RegisterRequestUseCase(applicationRepository, statusRepository,loanTypeRepository,
                transactionManager, authValidationGateway, findLoanTypeByIdUseCase, creditAnalysisGateway,
                customLogger);
    }

    @Bean
    UpdateApplicationStatusUseCase updateApplicationStatusUseCase(
            ApplicationRepository applicationRepository,
            StatusRepository statusRepository,
            NotificationGateway notificationGateway,
            ReportApprovedGateway reportApprovedGateway,
            CustomLogger customLogger
    ) {
        return new UpdateApplicationStatusUseCase(applicationRepository, statusRepository,notificationGateway,
                reportApprovedGateway, customLogger);
    }

    @Bean
    public PaymentPlanGenerator paymentPlanGenerator() {
        return new PaymentPlanGenerator();
    }
}
