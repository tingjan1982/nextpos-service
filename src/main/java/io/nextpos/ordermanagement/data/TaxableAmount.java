package io.nextpos.ordermanagement.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BigDecimal operations: https://www.baeldung.com/java-bigdecimal-biginteger
 */
@Data
@NoArgsConstructor
public class TaxableAmount {

    private BigDecimal taxRate;

    private boolean taxInclusive;

    private BigDecimal amountWithoutTax = BigDecimal.ZERO;

    private BigDecimal amountWithTax = BigDecimal.ZERO;

    private BigDecimal tax = BigDecimal.ZERO;

    TaxableAmount(final BigDecimal taxRate, boolean taxInclusive) {
        this.taxRate = taxRate;
        this.taxInclusive = taxInclusive;
    }

    public TaxableAmount newInstance() {
        return new TaxableAmount(this.taxRate, this.taxInclusive);
    }

    public void calculate(BigDecimal amount) {

        if (taxInclusive) {
            amountWithTax = amount;
            amountWithoutTax = amount.divide(BigDecimal.ONE.add(taxRate), 2, RoundingMode.UP);
            tax = amountWithTax.subtract(amountWithoutTax);
        } else {
            amountWithoutTax = amount;
            tax = amountWithoutTax.multiply(taxRate);
            amountWithTax = amountWithoutTax.add(tax);
        }
    }

    /**
     * @return amount that is appropriate for tax inclusive/exclusive scenarios.
     */
    public BigDecimal getAmount() {
        return isTaxInclusive() ? amountWithTax : amountWithoutTax;
    }

    public boolean isZero() {
        return amountWithoutTax.compareTo(BigDecimal.ZERO) == 0;
    }

    public TaxableAmount copy() {
        final TaxableAmount copy = this.newInstance();
        copy.amountWithoutTax = amountWithoutTax;
        copy.amountWithTax = amountWithTax;
        copy.tax = tax;

        return copy;
    }
}
