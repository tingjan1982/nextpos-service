package io.nextpos.settings.data;

import io.nextpos.shared.model.BaseObject;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

@Entity(name = "country_settings")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class CountrySettings extends BaseObject {

    @Id
    private String isoCountryCode;

    private BigDecimal taxRate;

    private Boolean taxInclusive;

    private Currency currency;

    private int decimalPlaces;

    @Enumerated(EnumType.STRING)
    private RoundingMode roundingMode;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "attribute")
    @CollectionTable(name = "country_settings_attributes", joinColumns = @JoinColumn(name = "iso_country_code"))
    private Set<String> commonAttributes = new HashSet<>();


    public CountrySettings(String isoCountryCode, BigDecimal taxRate, boolean taxInclusive, Currency currency, int decimalPlaces, RoundingMode roundingMode) {
        this.isoCountryCode = isoCountryCode;
        this.taxInclusive = taxInclusive;
        this.taxRate = taxRate;
        this.currency = currency;
        this.decimalPlaces = decimalPlaces;
        this.roundingMode = roundingMode;
    }

    public CountrySettings addCommonAttribute(String attribute) {
        this.commonAttributes.add(attribute);
        return this;
    }

    public RoundingAmountHelper roundingAmountHelper() {
        return new RoundingAmountHelper(decimalPlaces, roundingMode);
    }

    @Data
    @AllArgsConstructor
    public static class RoundingAmountHelper {

        private int decimalPlaces;

        private RoundingMode roundingMode;

        public BigDecimal roundAmount(Supplier<BigDecimal> amount) {
            return amount.get().setScale(decimalPlaces, roundingMode);
        }

        public String roundAmountAsString(Supplier<BigDecimal> amount) {
            return amount.get().setScale(decimalPlaces, roundingMode).toString();
        }
    }
}
