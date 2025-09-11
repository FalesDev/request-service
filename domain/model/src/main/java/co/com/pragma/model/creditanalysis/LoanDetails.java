package co.com.pragma.model.creditanalysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDetails {
    private Double amount;
    private Integer term;
    private Double interestRate;
    private String estado;
}
