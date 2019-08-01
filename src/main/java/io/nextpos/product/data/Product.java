package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.BusinessObjectState;
import io.nextpos.shared.model.ParentObject;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;

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
    private Client client;

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private ProductLabel productLabel;

    @OneToMany(mappedBy = "product", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @MapKeyEnumerated(EnumType.STRING)
    private Map<Version, ProductVersion> versions = new HashMap<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<ProductOptionRelation.ProductOptionOfProduct> productOptionOfProducts = new ArrayList<>();


    public Product(final Client client, final ProductVersion latestVersion) {
        this.client = client;

        latestVersion.setState(BusinessObjectState.DESIGN);
        latestVersion.setVersion(1);
        latestVersion.setProduct(this);

        versions.put(Version.DESIGN, latestVersion);
    }

    public ProductVersion getDesignVersion() {
        return versions.get(Version.DESIGN);
    }

    public ProductVersion getLiveVersion() {
        return getObjectByVersion(Version.LIVE).orElseThrow(() -> {
            throw new ObjectNotFoundException(Version.LIVE.name(), ProductVersion.class);
        });
    }

    @Override
    public Optional<ProductVersion> getObjectByVersion(final Version version) {
        return Optional.ofNullable(versions.get(version));
    }

    @Override
    public void deploy() {
        final ProductVersion latestVersion = getDesignVersion();
        latestVersion.setState(BusinessObjectState.DEPLOYED);

        final ProductVersion newLatest = latestVersion.copy();
        newLatest.setState(BusinessObjectState.DESIGN);
        newLatest.setProduct(this);

        versions.put(Version.LIVE, latestVersion);
        versions.put(Version.DESIGN, newLatest);
    }
}
