package co.com.pragma.model.creditanalysis;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PaymentDetail {
    private int month;
    private double payment;
    private double principal;
    private double interest;
    private double remainingBalance;
}
