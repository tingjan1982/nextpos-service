package io.nextpos.ordermanagement.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class ProductSnapshot {

    private String id;

    private String name;

    private String sku;

    private BigDecimal price;

    /**
     * Stores the discounted price with options after product level offer computation.
     */
    private BigDecimal discountedPrice;

    private String labelId;

    private String label;

    private List<ProductOptionSnapshot> productOptions = new ArrayList<>();

    public ProductSnapshot(final String id, final String name, final String sku, final BigDecimal price, final List<ProductOptionSnapshot> productOptions) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.price = price != null ? price : BigDecimal.ZERO;

        if (!CollectionUtils.isEmpty(productOptions)) {
            this.productOptions = productOptions;
        }
    }

    /**
     * Optionally set label information if product belongs to one.
     *
     * @param labelId
     * @param label
     */
    public void setLabelInformation(String labelId, String label) {
        this.labelId = labelId;
        this.label = label;
    }

    public BigDecimal getProductPriceWithOptions() {

        final BigDecimal optionPriceTotal = this.getProductOptions().stream()
                .filter(po -> BigDecimal.ZERO.compareTo(po.getOptionPrice()) < 0)
                .map(ProductSnapshot.ProductOptionSnapshot::getOptionPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return this.price.add(optionPriceTotal);
    }

    public ProductSnapshot copy() {
        final ProductSnapshot copy = new ProductSnapshot();
        copy.id = id;
        copy.name = name;
        copy.sku = sku;
        copy.price = price;
        copy.discountedPrice = discountedPrice;
        copy.labelId = labelId;
        copy.label = label;

        copy.productOptions = productOptions.stream().map(ProductOptionSnapshot::copy).collect(Collectors.toList());

        return copy;
    }

    @Data
    @NoArgsConstructor
    public static class ProductOptionSnapshot {

        private String optionName;

        private String optionValue;

        private BigDecimal optionPrice;

        public ProductOptionSnapshot(final String optionName, final String optionValue) {
            this(optionName, optionValue, BigDecimal.ZERO);
        }

        public ProductOptionSnapshot(final String optionName, final String optionValue, final BigDecimal optionPrice) {
            this.optionName = optionName;
            this.optionValue = optionValue;
            this.optionPrice = optionPrice != null ? optionPrice : BigDecimal.ZERO;
        }

        public ProductOptionSnapshot copy() {
            return new ProductOptionSnapshot(optionName, optionValue, optionPrice);
        }
    }

}
