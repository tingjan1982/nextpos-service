package io.nextpos.ordermanagement.data;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TaxableAmountTest {

    private BigDecimal taxRate = BigDecimal.valueOf(0.05);

    @Test
    void calculate_taxInclusive() {

        final TaxableAmount taxableAmount = new TaxableAmount(taxRate, true);
        taxableAmount.calculate(BigDecimal.valueOf(105));

        assertThat(taxableAmount.getAmountWithTax()).isEqualByComparingTo("105");
        assertThat(taxableAmount.getAmountWithoutTax()).isEqualByComparingTo("100");
        assertThat(taxableAmount.getTax()).isEqualByComparingTo("5");

        final TaxableAmount anotherTaxableAmount = taxableAmount.newInstance();
        assertThat(anotherTaxableAmount.getTaxRate()).isEqualByComparingTo(taxableAmount.getTaxRate());
        assertThat(anotherTaxableAmount.isTaxInclusive()).isEqualTo(taxableAmount.isTaxInclusive());
        assertThat(anotherTaxableAmount).isNotEqualTo(taxableAmount);

        taxableAmount.calculate(BigDecimal.valueOf(110));

        assertThat(taxableAmount.getAmountWithTax()).isEqualByComparingTo("110");
        assertThat(taxableAmount.getAmountWithoutTax()).isEqualByComparingTo("104.77");
        assertThat(taxableAmount.getTax()).isEqualByComparingTo("5.23");
    }

    @Test
    void calculate_taxExclusive() {

        final TaxableAmount taxableAmount = new TaxableAmount(taxRate, false);
        taxableAmount.calculate(BigDecimal.valueOf(100));

        assertThat(taxableAmount.getAmountWithTax()).isEqualByComparingTo("105");
        assertThat(taxableAmount.getAmountWithoutTax()).isEqualByComparingTo("100");
        assertThat(taxableAmount.getTax()).isEqualByComparingTo("5");
    }
}