package io.nextpos.ordermanagement.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ProductSnapshot {

    private String id;

    private String name;

    private String sku;

    private BigDecimal price;

    private List<ProductOptionSnapshot> productOptions = new ArrayList<>();

    public ProductSnapshot(final String id, final String name, final String sku, final BigDecimal price, final List<ProductOptionSnapshot> productOptions) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.price = price;
        this.productOptions = productOptions;
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
            this.optionPrice = optionPrice;
        }
    }

}
