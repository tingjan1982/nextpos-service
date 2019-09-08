package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ParentObject;
import io.nextpos.workingarea.data.WorkingArea;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Information on mapping a a one-to-many relationship with a Map.
 * https://stackoverflow.com/questions/25439813/difference-between-mapkey-mapkeycolumn-and-mapkeyjoincolumn-in-jpa-and-hiber
 *
 */
@Entity(name = "client_product")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
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

    @OneToMany(mappedBy = "product", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @MapKeyEnumerated(EnumType.STRING)
    private Map<Version, ProductVersion> versions = new HashMap<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<ProductOptionRelation.ProductOptionOfProduct> productOptionOfProducts = new ArrayList<>();


    public Product(final Client client, final ProductVersion latestVersion) {
        this.client = client;

        latestVersion.setVersion(Version.DESIGN);
        latestVersion.setVersionNumber(1);
        latestVersion.setProduct(this);

        versions.put(Version.DESIGN, latestVersion);
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
     *
     * Remove existing product options by setting parent to null in ProductOptionOfProduct.
     */
    public void replaceProductOptions(ProductOption... productOptions) {

        productOptionOfProducts.forEach(pop -> pop.setProduct(null));

        productOptionOfProducts.clear();

        if (productOptions != null) {
            Stream.of(productOptions).forEach(this::addProductOption);
        }
    }

    /**
     * Add ProductOption to this product is not already added.
     *
     * @param productOption
     * @return
     */
    public ProductOptionRelation.ProductOptionOfProduct addProductOption(ProductOption productOption) {

        final ProductOptionRelation.ProductOptionOfProduct productOptionRelation = new ProductOptionRelation.ProductOptionOfProduct(productOption, this);
        productOptionOfProducts.add(productOptionRelation);

        return productOptionRelation;
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
}
