package io.nextpos.settings.data;

import io.nextpos.shared.model.BaseObject;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

@Entity(name = "country_settings")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CountrySettings extends BaseObject {

    @Id
    private String isoCountryCode;

    private BigDecimal taxRate;

    private Boolean taxInclusive;

    private Currency currency;

    private int decimalPlaces;

    @Enumerated(EnumType.STRING)
    private RoundingMode roundingMode;

    private String dialingCode;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "attribute")
    @CollectionTable(name = "country_settings_attributes", joinColumns = @JoinColumn(name = "iso_country_code"))
    private Set<String> commonAttributes = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns = @JoinColumn(name = "country_code"), inverseJoinColumns = @JoinColumn(name = "payment_method_id"))
    @OrderBy("ordering asc")
    @Fetch(FetchMode.SUBSELECT)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<PaymentMethod> supportedPaymentMethods = new LinkedHashSet<>();

    public CountrySettings(String isoCountryCode, BigDecimal taxRate, boolean taxInclusive, Currency currency, int decimalPlaces, RoundingMode roundingMode, String dialingCode) {
        this.isoCountryCode = isoCountryCode;
        this.taxInclusive = taxInclusive;
        this.taxRate = taxRate;
        this.currency = currency;
        this.decimalPlaces = decimalPlaces;
        this.roundingMode = roundingMode;
        this.dialingCode = dialingCode;
    }

    public CountrySettings addCommonAttribute(String attribute) {
        this.commonAttributes.add(attribute);
        return this;
    }

    public void addSupportedPaymentMethod(PaymentMethod paymentMethod) {
        this.supportedPaymentMethods.add(paymentMethod);
    }

    public void clearPaymentMethods() {
        this.supportedPaymentMethods.clear();
    }

    public RoundingAmountHelper roundingAmountHelper() {
        return new RoundingAmountHelper(decimalPlaces, roundingMode);
    }

    public String formatPhoneNumber(String phoneNumber) {
        return String.format("+%s%s", dialingCode, phoneNumber.substring(1));
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
