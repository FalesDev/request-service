package co.com.pragma.sqs.sender.adapter;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.creditanalysis.CreditAnalysisResponsePayload;
import co.com.pragma.model.gateways.NotificationGateway;
import co.com.pragma.sqs.sender.SQSSender;
import co.com.pragma.sqs.sender.factory.SqsMessageFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class SqsNotificationAdapter implements NotificationGateway {

    private final SQSSender sqsSender;
    private final SqsMessageFactory messageFactory;
    private final String notificationsQueue;

    public SqsNotificationAdapter(
            SQSSender sqsSender,
            SqsMessageFactory messageFactory,
            @Value("${queue.names.notifications}") String notificationsQueue
    ) {
        this.sqsSender = sqsSender;
        this.messageFactory = messageFactory;
        this.notificationsQueue = notificationsQueue;
    }

    @Override
    public Mono<Void> sendDecisionNotification(Application application, String status) {
        var payload = Map.of(
                "applicationId", application.getId(),
                "email", application.getEmail(),
                "status", status,
                "amount", application.getAmount(),
                "term", application.getTerm()
        );

        var attributes = Map.of(
                "eventType", "DECISION_FINAL",
                "applicationId", application.getId().toString()
        );

        return sqsSender.send(
                notificationsQueue,
                messageFactory.toJson(payload),
                messageFactory.buildAttributes(attributes)
        ).then();
    }

    @Override
    public Mono<Void> sendCreditAnalysisDecisionNotification(CreditAnalysisResponsePayload payload) {
        var attributes = Map.of(
                "eventType", "CREDIT_ANALYSIS_RESPONSE",
                "applicationId", payload.getApplicationId().toString()
        );

        return sqsSender.send(
                notificationsQueue,
                messageFactory.toJson(payload),
                messageFactory.buildAttributes(attributes)
        ).then();
    }
}
