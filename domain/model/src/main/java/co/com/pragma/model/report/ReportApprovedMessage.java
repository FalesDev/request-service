package co.com.pragma.model.report;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class ReportApprovedMessage {
    private UUID applicationId;
    private Double amount;
    private String state;
}
