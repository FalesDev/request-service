package co.com.pragma.model.creditanalysis;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditAnalysisResponsePayload {
    private UUID applicationId;
    private String email;
    private String status;
    private Double amount;
    private Integer term;
    private List<PaymentDetail> paymentPlan;
}
