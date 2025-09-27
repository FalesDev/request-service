package co.com.pragma.usecase.findapprovedapplicationdaily;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.application.gateways.ApplicationRepository;
import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.gateways.ApplicationConfigurationProvider;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.report.DailyReport;
import co.com.pragma.model.status.Status;
import co.com.pragma.model.status.gateways.StatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindApprovedApplicationDailyUseCaseTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private StatusRepository statusRepository;
    @Mock
    private ApplicationConfigurationProvider configAdapter;
    @Mock
    private CustomLogger logger;

    @InjectMocks
    private FindApprovedApplicationDailyUseCase useCase;

    private static final String TIMEZONE = "America/Bogota";
    private static final String APPROVED_STATUS_NAME = "Approved";

    @BeforeEach
    void setUp() {
        when(configAdapter.getTimezone()).thenReturn(TIMEZONE);
    }

    @Test
    void shouldReturnDailyReportWhenApplicationsAreFound() {
        Status approvedStatus = Status.builder().id(UUID.randomUUID()).name(APPROVED_STATUS_NAME).build();
        when(statusRepository.findByNameIgnoreCase(APPROVED_STATUS_NAME))
                .thenReturn(Mono.just(approvedStatus));

        Application app1 = Application.builder().id(UUID.randomUUID()).amount(1000.0).build();
        Application app2 = Application.builder().id(UUID.randomUUID()).amount(2500.0).build();

        when(applicationRepository.findByStatusAndApprovedDateBetween(
                eq(approvedStatus.getId()),
                any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(Flux.just(app1, app2));

        Mono<DailyReport> result = useCase.findApprovedApplicationDaily();

        StepVerifier.create(result)
                .expectNextMatches(report ->
                        report.getApprovedLoansCount() == 2L &&
                                report.getTotalLoanAmount() == 3500.0
                )
                .verifyComplete();

        verify(statusRepository).findByNameIgnoreCase(APPROVED_STATUS_NAME);
        verify(applicationRepository).findByStatusAndApprovedDateBetween(any(UUID.class),
                any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void shouldReturnEmptyDailyReportWhenNoApplicationsAreFound() {
        Status approvedStatus = Status.builder().id(UUID.randomUUID()).name(APPROVED_STATUS_NAME).build();
        when(statusRepository.findByNameIgnoreCase(APPROVED_STATUS_NAME))
                .thenReturn(Mono.just(approvedStatus));

        when(applicationRepository.findByStatusAndApprovedDateBetween(
                eq(approvedStatus.getId()),
                any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(Flux.empty());

        Mono<DailyReport> result = useCase.findApprovedApplicationDaily();

        StepVerifier.create(result)
                .expectNextMatches(report ->
                        report.getApprovedLoansCount() == 0L &&
                                report.getTotalLoanAmount() == 0.0
                )
                .verifyComplete();

        verify(applicationRepository).findByStatusAndApprovedDateBetween(any(UUID.class),
                any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void shouldReturnErrorWhenApprovedStatusIsNotFound() {
        when(statusRepository.findByNameIgnoreCase(APPROVED_STATUS_NAME))
                .thenReturn(Mono.empty());
        Mono<DailyReport> result = useCase.findApprovedApplicationDaily();

        StepVerifier.create(result)
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(applicationRepository, never())
                .findByStatusAndApprovedDateBetween(any(UUID.class),
                        any(LocalDateTime.class), any(LocalDateTime.class));
    }
}