package io.nextpos.product.data;

import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.BusinessObjectState;
import io.nextpos.shared.model.ObjectVersioning;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "product_option_version")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ProductOptionVersion extends BaseObject implements ObjectVersioning<ProductOption> {

    @Id
    @GenericGenerator(name = "versionable", strategy = "io.nextpos.shared.model.idgenerator.ObjectVersionIdGenerator")
    @GeneratedValue(generator = "versionable")
    private String id;

    @ManyToOne
    private ProductOption productOption;

    private BusinessObjectState state;

    private String optionName;

    private OptionType optionType;

    @OneToMany(mappedBy = "productOption", cascade = CascadeType.ALL)
    private List<ProductOptionValue> optionValues = new ArrayList<>();


    public ProductOptionVersion(final String optionName, final OptionType optionType) {
        this.optionName = optionName;
        this.optionType = optionType;
    }

    public void addOptionValue(String optionValue) {
        this.addOptionValue(optionValue, BigDecimal.ZERO);
    }

    public void addOptionValue(String optionValue, BigDecimal optionalPrice) {

        if (optionType != OptionType.FREE_TEXT) {
            final ProductOptionValue optionValueObj = new ProductOptionValue(optionValue, optionalPrice);
            optionValueObj.setProductOption(this);

            optionValues.add(optionValueObj);
        }
    }

    @Override
    public ProductOption getParent() {
        return productOption;
    }


    @Entity(name = "product_option_value")
    @Data
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    public static class ProductOptionValue {

        @Id
        @GeneratedValue
        private Long id;

        @ManyToOne
        private ProductOptionVersion productOption;

        private String optionValue;

        private BigDecimal optionPrice;

        ProductOptionValue(final String optionValue, final BigDecimal optionPrice) {
            this.optionValue = optionValue;
            this.optionPrice = optionPrice;
        }
    }


    public enum OptionType {

        ONE_CHOICE, MULTIPLE_CHOICE, FREE_TEXT
    }
}
