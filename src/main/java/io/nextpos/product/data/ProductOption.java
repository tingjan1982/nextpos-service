package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ParentObject;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;

/**
 * https://www.baeldung.com/jpa-one-to-one
 */
@Entity(name = "client_product_option")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ProductOption extends BaseObject implements ParentObject<String, ProductOptionVersion> {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Client client;

    @OneToMany(mappedBy = "productOption", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @MapKeyEnumerated(EnumType.STRING)
    private Map<Version, ProductOptionVersion> versions = new HashMap<>();

    @OneToMany(mappedBy = "productOption", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<ProductOptionRelation.ProductOptionOfProduct> productOptionOfProducts = new ArrayList<>();

    
    public ProductOption(final Client client, final ProductOptionVersion latestProductOption) {
        this.client = client;

        latestProductOption.setVersion(1);
        latestProductOption.setProductOption(this);

        versions.put(Version.DESIGN, latestProductOption);
    }

    @Override
    public ProductOptionVersion getDesignVersion() {
        return versions.get(Version.DESIGN);
    }

    @Override
    public ProductOptionVersion getLiveVersion() {
        return versions.get(Version.LIVE);
    }

    @Override
    public Optional<ProductOptionVersion> getObjectByVersion(final Version version) {
        return Optional.ofNullable(versions.get(version));
    }

    public void deploy() {

        final ProductOptionVersion latestProductOption = this.getDesignVersion();
        final ProductOptionVersion newLatest = latestProductOption.copy();
        newLatest.setProductOption(this);

        versions.put(Version.LIVE, latestProductOption);
        versions.put(Version.DESIGN, newLatest);
    }

}
