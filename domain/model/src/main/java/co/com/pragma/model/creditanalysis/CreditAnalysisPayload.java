package co.com.pragma.model.creditanalysis;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditAnalysisPayload {
    private UUID idApplication;
    private UUID idUser;
    private String idDocument;
    private String email;
    private Double baseSalary;
    private LoanDetails newLoanDetails;
    private List<LoanDetails> loanAssets;
}
