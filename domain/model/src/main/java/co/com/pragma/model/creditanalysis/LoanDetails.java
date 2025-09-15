package co.com.pragma.model.creditanalysis;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDetails {
    private Double amount;
    private Integer term;
    private Double interestRate;
    private String estado;
}
