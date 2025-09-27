package co.com.pragma.sqs.listener;

import co.com.pragma.model.creditanalysis.ApplicationDecisionMessage;
import co.com.pragma.usecase.processapplicationdecision.ProcessApplicationDecisionUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.Message;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SQSProcessorTest {

    @Mock
    private ProcessApplicationDecisionUseCase processApplicationDecisionUseCase;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private SQSProcessor sqsProcessor;

    @BeforeEach
    void setUp() {
        sqsProcessor = new SQSProcessor(processApplicationDecisionUseCase, mapper);
    }

    @Test
    void apply_whenMessageIsValid_shouldProcessSuccessfully() throws JsonProcessingException {
        ApplicationDecisionMessage decisionMessage = new ApplicationDecisionMessage();
        decisionMessage.setApplicationId(UUID.randomUUID());
        decisionMessage.setDecision("APPROVED");
        decisionMessage.setTimestamp(Instant.now());

        String messageBody = mapper.writeValueAsString(decisionMessage);
        Message sqsMessage = Message.builder().body(messageBody).build();

        when(processApplicationDecisionUseCase.execute(any(ApplicationDecisionMessage.class)))
                .thenReturn(Mono.empty());

        Mono<Void> result = sqsProcessor.apply(sqsMessage);

        StepVerifier.create(result)
                .verifyComplete();

        verify(processApplicationDecisionUseCase).execute(any(ApplicationDecisionMessage.class));
    }

    @Test
    void apply_whenMessageBodyIsInvalid_shouldReturnMonoError() {
        String invalidJsonBody = "{\"applicationId\":\"123\", \"decision\":\"APPROVED\"";
        Message sqsMessage = Message.builder().body(invalidJsonBody).build();

        Mono<Void> result = sqsProcessor.apply(sqsMessage);

        StepVerifier.create(result)
                .expectError(JsonProcessingException.class)
                .verify();

        verify(processApplicationDecisionUseCase, never()).execute(any());
    }

    @Test
    void apply_whenUseCaseFails_shouldPropagateError() throws JsonProcessingException {
        ApplicationDecisionMessage decisionMessage = new ApplicationDecisionMessage();
        decisionMessage.setApplicationId(UUID.randomUUID());
        decisionMessage.setDecision("REJECTED");
        decisionMessage.setTimestamp(Instant.now());

        String messageBody = mapper.writeValueAsString(decisionMessage);
        Message sqsMessage = Message.builder().body(messageBody).build();

        RuntimeException expectedException = new RuntimeException("Error processing decision!");
        when(processApplicationDecisionUseCase.execute(any(ApplicationDecisionMessage.class)))
                .thenReturn(Mono.error(expectedException));

        Mono<Void> result = sqsProcessor.apply(sqsMessage);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Error processing decision!"))
                .verify();

        verify(processApplicationDecisionUseCase).execute(any(ApplicationDecisionMessage.class));
    }
}