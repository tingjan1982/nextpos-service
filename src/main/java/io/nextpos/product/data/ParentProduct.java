package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Entity(name = "client_parent_product")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ParentProduct extends Product {

    @ManyToOne
    private VariationDefinition variationDefinition;

    @OneToMany(mappedBy = "parentProduct", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @MapKey(name = "variation")
    @Fetch(FetchMode.SUBSELECT)
    private Map<String, ProductVariation> productVariations = new HashMap<>();

    protected ParentProduct(ParentProductBuilder builder) {
        super(builder);
        this.variationDefinition = builder.variationDefinition;
    }

    public static ParentProductBuilder builder(Client client, VariationDefinition variationDefinition) {
        return new ParentProductBuilder(client, variationDefinition);
    }

    public void addProductVariation(ProductVariation productVariation) {
        this.productVariations.put(productVariation.getVariation(), productVariation);
        productVariation.setParentProduct(this);
    }

    public Optional<ProductVariation> getProductVariation(String variationName) {
        return Optional.ofNullable(this.productVariations.get(variationName));
    }


    public static class ParentProductBuilder extends ProductBuilder<ParentProductBuilder, ParentProduct> {

        private final VariationDefinition variationDefinition;

        private final Map<String, BigDecimal> variationPrices = new HashMap<>();

        protected ParentProductBuilder(Client client, VariationDefinition variationDefinition) {
            super(client);
            this.variationDefinition = variationDefinition;
        }

        public ParentProductBuilder addVariation(String variation, BigDecimal variationPrice) {
            variationPrices.put(variation, variationPrice);
            return this;
        }

        @Override
        public ParentProduct build() {
            final ParentProduct parentProduct = new ParentProduct(this);

            variationDefinition.getAttributes().forEach(v -> {
                BigDecimal variationPrice = variationPrices.getOrDefault(v, this.price);
                final ProductVariation productVariation = ProductVariation.builder(client)
                        .copyFromProduct(parentProduct)
                        .variation(v, variationPrice).build();

                parentProduct.addProductVariation(productVariation);
            });

            return parentProduct;
        }

    }
}
