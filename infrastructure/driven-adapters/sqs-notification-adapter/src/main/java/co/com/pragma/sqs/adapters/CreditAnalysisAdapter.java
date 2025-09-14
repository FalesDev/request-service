package co.com.pragma.sqs.adapters;

import co.com.pragma.model.creditanalysis.CreditAnalysisPayload;
import co.com.pragma.model.creditanalysis.gateway.CreditAnalysisGateway;
import co.com.pragma.sqs.publisher.SqsEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CreditAnalysisAdapter implements CreditAnalysisGateway {

    private final SqsEventPublisher sqsEventPublisher;

    @Override
    public Mono<Void> requestAnalysis(CreditAnalysisPayload payload) {
        var attributes = Map.of(
                "applicationId", payload.getIdApplication().toString(),
                "idUser", payload.getIdUser().toString()
        );

        return sqsEventPublisher.publishEvent("CREDIT_ANALYSIS_REQUESTED", payload, attributes);
    }
}
