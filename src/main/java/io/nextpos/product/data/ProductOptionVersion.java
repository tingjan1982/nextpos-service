package io.nextpos.product.data;

import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ObjectVersioning;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity(name = "product_option_version")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ProductOptionVersion extends BaseObject implements ObjectVersioning<ProductOption> {

    @Id
    @GenericGenerator(name = "versionId", strategy = "io.nextpos.shared.model.idgenerator.ObjectVersionIdGenerator")
    @GeneratedValue(generator = "versionId")
    private String id;

    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ProductOption productOption;

    private int version;

    private String optionName;

    private OptionType optionType;

    @OneToMany(mappedBy = "productOption", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
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

    ProductOptionVersion copy() {

        final ProductOptionVersion productOptionCopy = new ProductOptionVersion(optionName, optionType);
        productOptionCopy.setVersion(version + 1);

        final List<ProductOptionValue> optionValuesCopy = optionValues.stream().map(po -> {
            final ProductOptionValue copy = po.copy();
            copy.setProductOption(productOptionCopy);
            return copy;
        }).collect(Collectors.toList());

        productOptionCopy.getOptionValues().addAll(optionValuesCopy);

        return productOptionCopy;
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
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        private ProductOptionVersion productOption;

        private String optionValue;

        private BigDecimal optionPrice;

        ProductOptionValue(final String optionValue, final BigDecimal optionPrice) {
            this.optionValue = optionValue;
            this.optionPrice = optionPrice;
        }

        ProductOptionValue copy() {
            return new ProductOptionValue(optionValue, optionPrice);
        }
    }


    public enum OptionType {
        ONE_CHOICE, MULTIPLE_CHOICE, FREE_TEXT
    }
}
