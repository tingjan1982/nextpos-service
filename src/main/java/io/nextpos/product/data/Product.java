package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ParentObject;
import io.nextpos.workingarea.data.WorkingArea;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * Information on mapping a a one-to-many relationship with a Map.
 * https://stackoverflow.com/questions/25439813/difference-between-mapkey-mapkeycolumn-and-mapkeyjoincolumn-in-jpa-and-hiber
 */
@Entity(name = "client_product")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Product extends BaseObject implements ParentObject<String, ProductVersion> {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Client client;

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private ProductLabel productLabel;

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private WorkingArea workingArea;

    private boolean outOfStock;

    private boolean pinned;

    @OneToMany(mappedBy = "product", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @MapKeyEnumerated(EnumType.STRING)
    private Map<Version, ProductVersion> versions = new HashMap<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<ProductOptionRelation.ProductOptionOfProduct> productOptionOfProducts = new ArrayList<>();

    protected Product(ProductBuilder<?, ?> builder) {
        this(builder.client, builder.productVersion());
    }

    public Product(final Client client, final ProductVersion latestVersion) {
        this.client = client;

        latestVersion.setVersion(Version.DESIGN);
        latestVersion.setVersionNumber(1);
        latestVersion.setProduct(this);

        versions.put(Version.DESIGN, latestVersion);
    }

    public static ProductBuilder<?, ?> builder(Client client) {
        return new ProductBuilder<>(client);
    }

    public ProductVersion getDesignVersion() {
        return getObjectByVersionThrows(Version.DESIGN);
    }

    public ProductVersion getLiveVersion() {
        return getObjectByVersionThrows(Version.LIVE);
    }

    @Override
    public Optional<ProductVersion> getObjectByVersion(final Version version) {
        return Optional.ofNullable(versions.get(version));
    }

    /**
     * Clears all existing product option relation and re-add those passed through as parameter.
     * <p>
     * Remove existing product options by setting parent to null in ProductOptionOfProduct.
     */
    public void replaceProductOptions(ProductOption... productOptions) {
        ProductOptionHelper.replaceProductOptions(productOptionOfProducts,
                this::addProductOption,
                productOptions);
    }

    private ProductOptionRelation.ProductOptionOfProduct addProductOption(ProductOption productOption) {
        return new ProductOptionRelation.ProductOptionOfProduct(productOption, this);
    }

    @Override
    public void deploy() {

        getObjectByVersion(Version.LIVE).ifPresent(liveVersion -> liveVersion.setVersion(Version.RETIRED));

        final ProductVersion latestVersion = getDesignVersion();
        latestVersion.setVersion(Version.LIVE);

        final ProductVersion newLatest = latestVersion.copy();
        newLatest.setProduct(this);

        versions.put(Version.LIVE, latestVersion);
        versions.put(Version.DESIGN, newLatest);
    }

    public static class ProductBuilder<T extends ProductBuilder<T, P>, P extends Product> {

        protected final Client client;

        protected String productName;

        protected String internalProductName;

        protected String sku;

        protected String description;

        protected BigDecimal price;

        protected BigDecimal costPrice;

        protected ProductBuilder(Client client) {
            this.client = client;
        }

        public T productNameAndPrice(String productName, BigDecimal price) {
            this.productName = productName;
            this.price = price;

            return (T) this;
        }

        public T internalProductName(String internalProductName) {
            this.internalProductName = internalProductName;
            return (T) this;
        }
        
        public T sku(String sku) {
            this.sku = sku;
            return (T) this;
        }

        public T description(String description) {
            this.description = description;
            return (T) this;
        }

        public T price(BigDecimal price) {
            this.price = price;
            return (T) this;
        }

        public T costPrice(BigDecimal costPrice) {
            this.costPrice = costPrice;
            return (T) this;
        }

        public ProductVersion productVersion() {
            final ProductVersion productVersion = new ProductVersion(productName, sku, description, price);
            productVersion.setInternalProductName(internalProductName);
            productVersion.setCostPrice(costPrice);

            return productVersion;
        }

        public T copyFromProduct(Product product) {
            final ProductVersion productVersion = product.getDesignVersion();
            this.productName = productVersion.getProductName();
            this.internalProductName = productVersion.getInternalProductName();
            this.sku = productVersion.getSku();
            this.description = productVersion.getDescription();
            this.price = productVersion.getPrice();
            this.costPrice = productVersion.getCostPrice();

            return (T) this;
        }

        public P build() {
            return (P) new Product(this);
        }
    }
}
