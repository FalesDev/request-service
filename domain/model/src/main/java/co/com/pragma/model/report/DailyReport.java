package co.com.pragma.model.report;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class DailyReport {
    private Long approvedLoansCount;
    private Double totalLoanAmount;
}
