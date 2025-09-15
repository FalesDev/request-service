package co.com.pragma.model.creditanalysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class PaymentPlanGenerator {

    private double calculateMonthlyInstallment(double principal, double annualInterestRate, int termInMonths) {
        if (annualInterestRate <= 0) {
            return principal / termInMonths;
        }
        double monthlyRate = annualInterestRate / 12 / 100;
        double numerator = monthlyRate * Math.pow(1 + monthlyRate, termInMonths);
        double denominator = Math.pow(1 + monthlyRate, termInMonths) - 1;
        return principal * (numerator / denominator);
    }

    public List<PaymentDetail> generate(double principal, double annualInterestRate, int termInMonths) {
        BigDecimal monthlyPayment = BigDecimal.valueOf(calculateMonthlyInstallment(principal, annualInterestRate, termInMonths));
        BigDecimal balance = BigDecimal.valueOf(principal);
        BigDecimal monthlyRate = BigDecimal.valueOf(annualInterestRate / 12 / 100);
        List<PaymentDetail> payments = new ArrayList<>();

        for (int month = 1; month <= termInMonths; month++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPayment;
            BigDecimal currentPayment;

            if (month == termInMonths) {
                principalPayment = balance;
                currentPayment = principalPayment.add(interest);
                balance = BigDecimal.ZERO;
            } else {
                principalPayment = monthlyPayment.subtract(interest);
                currentPayment = monthlyPayment;
                balance = balance.subtract(principalPayment);
            }

            payments.add(PaymentDetail.builder()
                    .month(month)
                    .payment(currentPayment.setScale(2, RoundingMode.HALF_UP).doubleValue())
                    .principal(principalPayment.setScale(2, RoundingMode.HALF_UP).doubleValue())
                    .interest(interest.doubleValue())
                    .remainingBalance(balance.setScale(2, RoundingMode.HALF_UP).doubleValue())
                    .build());
        }
        return payments;
    }
}
