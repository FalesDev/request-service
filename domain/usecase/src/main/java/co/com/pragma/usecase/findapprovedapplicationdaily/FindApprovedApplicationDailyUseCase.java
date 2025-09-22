package co.com.pragma.usecase.findapprovedapplicationdaily;

import co.com.pragma.model.application.gateways.ApplicationRepository;
import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.gateways.ApplicationConfigurationProvider;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.report.DailyReport;
import co.com.pragma.model.status.gateways.StatusRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@RequiredArgsConstructor
public class FindApprovedApplicationDailyUseCase {

    private final ApplicationRepository applicationRepository;
    private final StatusRepository statusRepository;
    private final ApplicationConfigurationProvider  configAdapter;
    private final CustomLogger logger;

    private static final String APPROVED_STATUS_NAME = "Approved";
    private static final LocalTime CUT_OFF_TIME = LocalTime.of(3, 0);

    public Mono<DailyReport> findApprovedApplicationDaily() {
        logger.trace("Finding Approved Application for daily report");

        ZoneId zoneId = ZoneId.of(configAdapter.getTimezone());

        LocalDateTime endDateTime = LocalDateTime.of(LocalDate.now(zoneId), CUT_OFF_TIME);
        LocalDateTime startDateTime = endDateTime.minusDays(1);

        return statusRepository.findByNameIgnoreCase(APPROVED_STATUS_NAME)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Status not found")))
                .flatMap(status -> applicationRepository.findByStatusAndApprovedDateBetween(status.getId(),
                                startDateTime,
                                endDateTime)
                        .collect(
                                () -> DailyReport.builder().approvedLoansCount(0L).totalLoanAmount(0.0).build(),
                                (report, application) -> {
                                    report.setApprovedLoansCount(report.getApprovedLoansCount() + 1);
                                    report.setTotalLoanAmount(report.getTotalLoanAmount() + application.getAmount());
                                }
                        )
                );
    }
}
