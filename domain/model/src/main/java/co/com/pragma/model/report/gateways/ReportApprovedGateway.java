package co.com.pragma.model.report.gateways;

import co.com.pragma.model.application.Application;
import reactor.core.publisher.Mono;

public interface ReportApprovedGateway {
    Mono<Void> sendReportApprovedCount(Application application, String status);
}
