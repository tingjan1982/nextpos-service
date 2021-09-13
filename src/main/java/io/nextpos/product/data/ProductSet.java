package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity(name = "client_product_set")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class ProductSet extends Product {

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "child_product_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "child_product_id"}))
    @Fetch(FetchMode.SUBSELECT)
    private Set<Product> childProducts = new HashSet<>();

    protected ProductSet(final ProductSetBuilder builder) {
        super(builder);
        childProducts = builder.childProducts;
    }

    public static ProductSetBuilder builder(Client client) {
        return new ProductSetBuilder(client);
    }

    public void clearChildProducts() {
        this.childProducts.clear();
    }

    public void addChildProduct(Product product) {
        this.childProducts.add(product);
    }

    public void removeChildProduct(Product product) {
        this.childProducts.remove(product);
    }


    public static class ProductSetBuilder extends ProductBuilder<ProductSetBuilder, ProductSet> {

        private final Set<Product> childProducts;

        protected ProductSetBuilder(Client client) {
            super(client);
            childProducts = new HashSet<>();
        }

        public ProductSetBuilder addChildProduct(Product childProduct) {
            childProducts.add(childProduct);
            return this;
        }

        public ProductSet build() {
            return new ProductSet(this);
        }
    }
}
