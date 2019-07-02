package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.BusinessObjectState;
import io.nextpos.shared.model.VersionableClientObject;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "client_product")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Product extends BaseObject implements VersionableClientObject<ProductVersion> {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne
    private Client client;

    @OneToOne(fetch = FetchType.EAGER)
    private ProductVersion latestVersion;

    //todo: address potential performance issue
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductVersion> productVersions = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<ProductOptionRelation.ProductOptionOfProduct> productOptionOfProducts = new ArrayList<>();


    public Product(final Client client, final ProductVersion latestVersion) {
        this.client = client;

        this.addNewVersion(latestVersion);
    }

    @Override
    public Client getClient() {
        return client;
    }

    @Override
    public void setClient(final Client client) {
        this.client = client;
    }

    @Override
    public ProductVersion getLatestVersion() {
        return latestVersion;
    }

    @Override
    public ProductVersion addNewVersion(final ProductVersion productVersion) {
        productVersion.setProduct(this);
        productVersion.setState(BusinessObjectState.DESIGN);

        int version = 1;

        if (latestVersion != null) {
            latestVersion.setState(BusinessObjectState.RETIRED);
            version = latestVersion.getVersion() + 1;
        }

        latestVersion = productVersion;
        productVersion.setVersion(version);

        productVersions.add(productVersion);

        return productVersion;
    }
}
