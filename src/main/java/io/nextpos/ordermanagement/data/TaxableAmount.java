package io.nextpos.ordermanagement.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * BigDecimal operations: https://www.baeldung.com/java-bigdecimal-biginteger
 */
@Data
@NoArgsConstructor
public class TaxableAmount {

    private BigDecimal taxRate;

    private BigDecimal amountWithoutTax = BigDecimal.ZERO;

    private BigDecimal amountWithTax = BigDecimal.ZERO;

    private BigDecimal tax = BigDecimal.ZERO;

    TaxableAmount(final BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public void calculate(BigDecimal amount) {

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            amountWithoutTax = amount;
            tax = amountWithoutTax.multiply(taxRate);
            amountWithTax = amountWithoutTax.add(tax);
        }
    }
}