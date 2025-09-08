package co.com.pragma.model.application;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ApplicationAdvisorView {
    private Double amount;
    private Integer term;
    private String email;
    private String fullName;
    private String loanTypeName;
    private Double interestRate;
    private String statusName;
    private Double baseSalary;
    private BigDecimal totalMonthlyDebt;
}
