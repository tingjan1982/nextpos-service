package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ObjectOrdering;
import io.nextpos.shared.model.ParentObject;
import io.nextpos.workingarea.data.WorkingArea;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * Information on mapping a a one-to-many relationship with a Map.
 *
 * https://stackoverflow.com/questions/25439813/difference-between-mapkey-mapkeycolumn-and-mapkeyjoincolumn-in-jpa-and-hiber
 *
 * Fetch Mode:
 * https://www.baeldung.com/hibernate-fetchmode
 */
@Entity(name = "client_product")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Product extends BaseObject implements ParentObject<String, ProductVersion>, ObjectOrdering<Integer> {

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

    private Integer ordering = 0;

    @OneToMany(mappedBy = "product", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(value = FetchMode.SUBSELECT)
    @MapKeyEnumerated(EnumType.STRING)
    private Map<Version, ProductVersion> versions = new HashMap<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<ProductOptionRelation.ProductOptionOfProduct> productOptionOfProducts = new ArrayList<>();

    protected Product(ProductBuilder<?, ?> builder) {
        this(builder.client, builder.productVersion());

        this.productLabel = builder.productLabel;
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

    public void updateSettingsFromProductLabel() {

        if (productLabel != null) {
            this.setWorkingArea(productLabel.getWorkingArea());
            final ProductOption[] productOptions = productLabel.getProductOptionOfLabels().stream()
                    .map(ProductOptionRelation.ProductOptionOfLabel::getProductOption).toArray(ProductOption[]::new);

            this.replaceProductOptions(productOptions);
        } 
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

        protected ProductLabel productLabel;

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

        public T productLabel(ProductLabel productLabel) {
            this.productLabel = productLabel;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Product product = (Product) o;
        return id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }
}
