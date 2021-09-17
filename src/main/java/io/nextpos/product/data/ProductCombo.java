package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.ObjectOrdering;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Entity(name = "client_product_combo")
@Getter
@Setter
@ToString(callSuper = true)
@RequiredArgsConstructor
public class ProductCombo extends Product {

    @OneToMany(mappedBy = "productCombo", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SELECT)
    @OrderBy("ordering ASC")
    private List<ProductComboLabel> productComboLabels = new ArrayList<>();
    
    protected ProductCombo(final ProductComboBuilder builder) {
        super(builder);
        productComboLabels = builder.productComboLabels;
    }

    public static ProductComboBuilder builder(Client client) {
        return new ProductComboBuilder(client);
    }

    public void clearProductComboLabels() {
        this.productComboLabels.clear();
    }

    public ProductComboLabel addProductComboLabel(ProductLabel label) {
        final ProductCombo.ProductComboLabel comboLabel = new ProductCombo.ProductComboLabel(this, label);
        this.productComboLabels.add(comboLabel);

        return comboLabel;
    }

    public Optional<ProductComboLabel> getProductComboLabel(ProductLabel label) {
        return this.productComboLabels.stream()
                .filter(l -> l.getProductLabel().equals(label))
                .findFirst();
    }


    public static class ProductComboBuilder extends ProductBuilder<ProductComboBuilder, ProductCombo> {

        private final List<ProductComboLabel> productComboLabels;

        protected ProductComboBuilder(Client client) {
            super(client);
            productComboLabels = new ArrayList<>();
        }

        public ProductComboBuilder addProductComboLabel(ProductComboLabel label) {
            productComboLabels.add(label);
            return this;
        }

        public ProductCombo build() {
            return new ProductCombo(this);
        }
    }


    @Entity(name = "client_product_combo_label")
    @Getter
    @Setter
    @ToString
    @RequiredArgsConstructor
    public static class ProductComboLabel implements ObjectOrdering<Integer> {

        @EmbeddedId
        private ProductComboLabelKey id;

        @ManyToOne
        @MapsId("productId")
        @JoinColumn(name = "product_id")
        @ToString.Exclude
        private ProductCombo productCombo;

        @ManyToOne
        @MapsId("productLabelId")
        @JoinColumn(name = "product_label_id")
        @ToString.Exclude
        private ProductLabel productLabel;

        private boolean multipleSelection;

        private Integer ordering = 0;

        public ProductComboLabel(ProductCombo productCombo, ProductLabel productLabel) {
            this.id = new ProductComboLabelKey(productCombo.getId(), productLabel.getId());
            this.productCombo = productCombo;
            this.productLabel = productLabel;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProductComboLabel that = (ProductComboLabel) o;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    @Embeddable
    @Getter
    @Setter
    @ToString
    @RequiredArgsConstructor
    public static class ProductComboLabelKey implements Serializable {

        @Column(name = "product_id")
        private String productId;

        @Column(name = "product_label_id")
        private String productLabelId;

        public ProductComboLabelKey(String productId, String productLabelId) {
            this.productId = productId;
            this.productLabelId = productLabelId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProductComboLabelKey that = (ProductComboLabelKey) o;
            return Objects.equals(productId, that.productId) && Objects.equals(productLabelId, that.productLabelId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productId, productLabelId);
        }
    }
}
