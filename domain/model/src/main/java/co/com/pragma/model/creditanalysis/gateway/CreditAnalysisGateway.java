package co.com.pragma.model.creditanalysis.gateway;

import co.com.pragma.model.creditanalysis.CreditAnalysisPayload;
import reactor.core.publisher.Mono;

public interface CreditAnalysisGateway {
    Mono<Void> requestAnalysis(CreditAnalysisPayload payload);
}
