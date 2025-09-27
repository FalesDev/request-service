package co.com.pragma.model.creditanalysis;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;

import java.util.List;

class PaymentPlanGeneratorTest {

    private final PaymentPlanGenerator generator = new PaymentPlanGenerator();

    @Test
    void shouldGeneratePlanWithInterest() {
        List<PaymentDetail> payments = generator.generate(1000, 12, 12);

        assertThat(payments).hasSize(12);
        assertThat(payments.getFirst().getInterest()).isGreaterThan(0);
        assertThat(payments.get(11).getRemainingBalance()).isEqualTo(0.00);

        double totalPrincipal = payments.stream().mapToDouble(PaymentDetail::getPrincipal).sum();
        assertThat(totalPrincipal).isCloseTo(1000, within(0.01));
    }

    @Test
    void shouldGeneratePlanWithZeroInterest() {
        List<PaymentDetail> payments = generator.generate(1200, 0, 12);

        assertThat(payments).hasSize(12);
        assertThat(payments.getFirst().getInterest()).isEqualTo(0.00);
        assertThat(payments.get(11).getRemainingBalance()).isEqualTo(0.00);
        assertThat(payments).allSatisfy(p ->
                assertThat(p.getPayment()).isEqualTo(100.00)
        );
    }

    @Test
    void shouldGeneratePlanWithNegativeInterest() {
        List<PaymentDetail> payments = generator.generate(600, -5, 6);

        assertThat(payments).hasSize(6);
        assertThat(payments.get(0).getInterest()).isLessThanOrEqualTo(0.0);
        assertThat(payments.get(5).getRemainingBalance()).isEqualTo(0.00);
    }

    @Test
    void shouldGeneratePlanWithSingleMonth() {
        List<PaymentDetail> payments = generator.generate(500, 10, 1);

        assertThat(payments).hasSize(1);
        assertThat(payments.getFirst().getPrincipal()).isEqualTo(500.00);
        assertThat(payments.getFirst().getInterest()).isGreaterThan(0.00);
        assertThat(payments.getFirst().getRemainingBalance()).isEqualTo(0.00);
    }

    @Test
    void shouldGeneratePlanWithTwoDecimals() {
        List<PaymentDetail> payments = generator.generate(1000, 5, 3);

        payments.forEach(p -> {
            assertThat(p.getPayment()).isEqualTo(Math.round(p.getPayment() * 100.0) / 100.0);
            assertThat(p.getRemainingBalance()).isGreaterThanOrEqualTo(0.00);
        });
    }
}
