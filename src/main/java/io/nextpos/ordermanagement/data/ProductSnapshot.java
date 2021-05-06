package io.nextpos.ordermanagement.data;

import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductVersion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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

    private String internalName;

    private String sku;

    private BigDecimal price;

    /**
     * This will override the original price and option prices.
     *
     * null means there is no override price.
     * zero means this is a free line item, else, it is an override price.
     */
    private BigDecimal overridePrice;

    /**
     * Stores the discounted price with options after product level offer computation.
     */
    private BigDecimal discountedPrice;

    private String labelId;

    private String label;

    private List<ChildProductSnapshot> childProducts = new ArrayList<>();

    private List<ProductOptionSnapshot> productOptions = new ArrayList<>();

    public ProductSnapshot(Product product) {
        final ProductVersion productVersion = product.getDesignVersion();

        this.id = product.getId();
        this.name = productVersion.getProductName();
        this.internalName = productVersion.getInternalProductName();
        this.price = productVersion.getPrice();
    }

    public ProductSnapshot(final String id, final String name, final BigDecimal price) {
        this(id, name, null, price, null);
    }

    public ProductSnapshot(final String id, final String name, String sku, final BigDecimal price, final List<ProductOptionSnapshot> productOptions) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.price = price != null ? price : BigDecimal.ZERO;

        if (!CollectionUtils.isEmpty(productOptions)) {
            this.productOptions = productOptions;
        }
    }

    public String getDisplayName() {
        return StringUtils.isNotBlank(internalName) ? internalName : name;
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

    public String getOverridePriceString() {
        return overridePrice != null ? overridePrice.toString() : null;
    }

    public BigDecimal getProductPriceWithOptions() {

        if (overridePrice != null) {
            return overridePrice;
        }

        final BigDecimal optionPriceTotal = this.getProductOptions().stream()
                .map(ProductSnapshot.ProductOptionSnapshot::getOptionPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return price.add(optionPriceTotal);
    }

    public ProductSnapshot copy() {
        final ProductSnapshot copy = new ProductSnapshot();
        copy.id = id;
        copy.name = name;
        copy.internalName = internalName;
        copy.sku = sku;
        copy.price = price;
        copy.overridePrice = overridePrice;
        copy.discountedPrice = discountedPrice;
        copy.labelId = labelId;
        copy.label = label;

        copy.productOptions = productOptions.stream().map(ProductOptionSnapshot::copy).collect(Collectors.toList());

        return copy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChildProductSnapshot {

        private String id;

        private String productName;

        private String internalProductName;

        public String getDisplayName() {
            return StringUtils.isNotBlank(internalProductName) ? internalProductName : productName;
        }
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
