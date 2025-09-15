package co.com.pragma.sqs.sender.adapter;

import co.com.pragma.model.creditanalysis.CreditAnalysisPayload;
import co.com.pragma.model.creditanalysis.gateway.CreditAnalysisGateway;
import co.com.pragma.sqs.sender.SQSSender;
import co.com.pragma.sqs.sender.factory.SqsMessageFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CreditAnalysisAdapter implements CreditAnalysisGateway {

    private final SQSSender sqsSender;
    private final SqsMessageFactory messageFactory;

    @Value("${queue.names.indebtedness}")
    private String indebtednessQueue;

    @Override
    public Mono<Void> requestAnalysis(CreditAnalysisPayload payload) {
        var attributes = Map.of(
                "eventType", "CREDIT_ANALYSIS_REQUESTED",
                "applicationId", payload.getIdApplication().toString(),
                "idUser", payload.getIdUser().toString()
        );

        return sqsSender.send(
                indebtednessQueue,
                messageFactory.toJson(payload),
                messageFactory.buildAttributes(attributes)
        ).then();
    }
}

