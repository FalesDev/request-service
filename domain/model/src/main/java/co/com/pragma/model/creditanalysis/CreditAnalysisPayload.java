package co.com.pragma.model.creditanalysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditAnalysisPayload {
    private UUID applicationId;
    private String idDocument;
    private String email;
    private Double baseSalary;
    private LoanDetails newLoanDetails;
    private List<LoanDetails> loanAssets;
}
