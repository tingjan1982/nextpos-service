package io.nextpos.settings.data;

import io.nextpos.shared.model.BaseObject;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashSet;
import java.util.Set;

@Entity(name = "country_settings")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class CountrySettings extends BaseObject {

    @Id
    private String isoCountryCode;

    private BigDecimal taxRate;

    private Currency currency;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "attribute")
    @CollectionTable(name = "country_settings_attributes", joinColumns = @JoinColumn(name = "iso_country_code"))
    private Set<String> commonAttributes = new HashSet<>();


    public CountrySettings(final String isoCountryCode, final BigDecimal taxRate, final Currency currency) {
        this.isoCountryCode = isoCountryCode;
        this.taxRate = taxRate;
        this.currency = currency;
    }

    public CountrySettings addCommonAttribute(String attribute) {
        this.commonAttributes.add(attribute);
        return this;
    }
}
