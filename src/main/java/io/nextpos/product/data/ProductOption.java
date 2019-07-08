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
import java.util.ArrayList;
import java.util.List;

/**
 * https://www.baeldung.com/jpa-one-to-one
 */
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
    @OneToOne(mappedBy = "productOption", fetch = FetchType.EAGER, cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
    private ProductOptionVersion latestProductOption;

    @OneToOne(mappedBy = "productOption", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private ProductOptionVersion deployedProductOption;

    @OneToMany(mappedBy = "productOption", cascade = CascadeType.ALL)
    private List<ProductOptionRelation.ProductOptionOfProduct> productOptionOfProducts = new ArrayList<>();

    
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
