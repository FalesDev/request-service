package co.com.pragma.sqs.adapters;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.gateways.NotificationGateway;
import co.com.pragma.sqs.publisher.SqsEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SqsNotificationAdapter implements NotificationGateway {

    private final SqsEventPublisher sqsEventPublisher;

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
                "applicationId", application.getId().toString()
        );

        return sqsEventPublisher.publishEvent("DECISION_FINAL", payload, attributes);
    }
}
