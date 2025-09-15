package co.com.pragma.sqs.listener;

import co.com.pragma.model.creditanalysis.ApplicationDecisionMessage;
import co.com.pragma.usecase.processapplicationdecision.ProcessApplicationDecisionUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class SQSProcessor implements Function<Message, Mono<Void>> {
    private final ProcessApplicationDecisionUseCase processApplicationDecisionUseCase;
    private final ObjectMapper mapper;

    @Override
    public Mono<Void> apply(Message message) {
        try {
            ApplicationDecisionMessage decisionMessage =
                    mapper.readValue(message.body(), ApplicationDecisionMessage.class);

            return processApplicationDecisionUseCase.execute(decisionMessage);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
