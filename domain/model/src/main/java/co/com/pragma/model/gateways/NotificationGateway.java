package co.com.pragma.model.gateways;

import co.com.pragma.model.application.Application;
import co.com.pragma.model.creditanalysis.CreditAnalysisResponsePayload;
import reactor.core.publisher.Mono;

public interface NotificationGateway {
    Mono<Void> sendDecisionNotification(Application application, String status);
    Mono<Void> sendCreditAnalysisDecisionNotification(CreditAnalysisResponsePayload creditAnalysisResponsePayload);
}
