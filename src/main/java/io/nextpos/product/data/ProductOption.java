package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ParentObject;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity(name = "client_product_option")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ProductOption extends BaseObject implements ParentObject<String> {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne
    private Client client;

    /**
     * There is always a staging version.
     */
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL,optional = false)
    private ProductOptionVersion latestProductOption;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private ProductOptionVersion deployedProductOption;


    public ProductOption(final Client client, final ProductOptionVersion latestProductOption) {
        this.client = client;
        this.latestProductOption = latestProductOption;

        this.latestProductOption.setVersion(1);
        this.latestProductOption.setProductOption(this);
    }

    public void deploy() {
        final ProductOptionVersion newLatest = latestProductOption.copy();
        newLatest.setProductOption(this);

        deployedProductOption = latestProductOption;

        latestProductOption = newLatest;
    }
}
