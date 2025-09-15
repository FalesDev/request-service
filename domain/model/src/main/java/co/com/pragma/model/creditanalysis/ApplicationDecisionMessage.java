package co.com.pragma.model.creditanalysis;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ApplicationDecisionMessage {
    private UUID applicationId;
    private String decision;
    private Instant timestamp;
}
