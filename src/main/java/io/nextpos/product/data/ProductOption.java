package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ParentObject;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    // todo: may consider using EntityGraph to load this collection eagerly. Commented out code is to allow product deletion to work successfully.
//    @OneToMany(mappedBy = "productOption")//, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//    @Fetch(value = FetchMode.SUBSELECT)
//    private List<ProductOptionRelation.ProductOptionOfProduct> productOptionOfProducts = new ArrayList<>();

    
    public ProductOption(final Client client, final ProductOptionVersion latestProductOption) {
        this.client = client;

        latestProductOption.setVersionNumber(1);
        latestProductOption.setVersion(Version.DESIGN);
        latestProductOption.setProductOption(this);

        versions.put(Version.DESIGN, latestProductOption);
    }

    @Override
    public ProductOptionVersion getDesignVersion() {
        return getObjectByVersionThrows(Version.DESIGN);
    }

    @Override
    public ProductOptionVersion getLiveVersion() {
        return getObjectByVersionThrows(Version.LIVE);
    }

    @Override
    public Optional<ProductOptionVersion> getObjectByVersion(final Version version) {
        return Optional.ofNullable(versions.get(version));
    }

    @Override
    public void deploy() {

        getObjectByVersion(Version.LIVE).ifPresent(liveVersion -> {
            liveVersion.setVersion(Version.RETIRED);

            // fix the integrity constraint violation: foreign key no action issue
            liveVersion.getOptionValues().forEach(pov -> pov.setProductOption(null));
        });

        final ProductOptionVersion latestProductOption = this.getDesignVersion();
        latestProductOption.setVersion(Version.LIVE);

        final ProductOptionVersion newLatest = latestProductOption.copy();
        newLatest.setProductOption(this);

        versions.put(Version.LIVE, latestProductOption);
        versions.put(Version.DESIGN, newLatest);
    }

}
