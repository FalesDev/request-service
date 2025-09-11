package co.com.pragma.model.gateways;

import co.com.pragma.model.application.Application;
import reactor.core.publisher.Mono;

public interface NotificationGateway {
    Mono<Void> sendDecisionNotification(Application application, String status);
}
