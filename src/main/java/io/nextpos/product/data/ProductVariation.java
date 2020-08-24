package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.math.BigDecimal;

@Entity(name = "client_product_variation")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ProductVariation extends Product {

    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ParentProduct parentProduct;

    private String variation;

    protected ProductVariation(ProductVariationBuilder builder) {
        super(builder);
        this.variation = builder.variation;
    }

    public static ProductVariationBuilder builder(Client client) {
        return new ProductVariationBuilder(client);
    }


    public static class ProductVariationBuilder extends ProductBuilder<ProductVariationBuilder, ProductVariation> {

        private String variation;

        private BigDecimal variationPrice;

        public ProductVariationBuilder(Client client) {
            super(client);
        }

        public ProductVariationBuilder variation(String variation, BigDecimal variationPrice) {
            this.variation = variation;
            this.variationPrice = variationPrice;

            return this;
        }

        @Override
        public ProductVariation build() {
            this.price(this.variationPrice);
            return new ProductVariation(this);
        }
    }
}
