package co.com.pragma.sqs.sender.adapter;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.report.ReportApprovedMessage;
import co.com.pragma.model.report.gateways.ReportApprovedGateway;
import co.com.pragma.sqs.sender.SQSSender;
import co.com.pragma.sqs.sender.factory.SqsMessageFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class ReportApprovedAdapter implements ReportApprovedGateway {

    private final SQSSender sqsSender;
    private final SqsMessageFactory messageFactory;
    private final String reportingQueue;

    public ReportApprovedAdapter(
            SQSSender sqsSender,
            SqsMessageFactory messageFactory,
            @Value("${queue.names.reporting}") String reportingQueue
    ) {
        this.sqsSender = sqsSender;
        this.messageFactory = messageFactory;
        this.reportingQueue = reportingQueue;
    }

    @Override
    public Mono<Void> sendReportApprovedCount(Application application, String status) {
        ReportApprovedMessage payload = ReportApprovedMessage.builder()
                .applicationId(application.getId())
                .amount(application.getAmount())
                .state(status)
                .build();

        var attributes = Map.of(
                "eventType", "REPORT_APPROVED",
                "reportId", "total_approved_requests"
        );

        return sqsSender.send(
                reportingQueue,
                messageFactory.toJson(payload),
                messageFactory.buildAttributes(attributes)
        ).then();
    }
}
