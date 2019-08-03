package io.nextpos.product.data;

import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ObjectVersioning;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity(name = "product_version")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ProductVersion extends BaseObject implements ObjectVersioning<Product> {

    @Id
    @GenericGenerator(name = "versionId", strategy = "io.nextpos.shared.model.idgenerator.ObjectVersionIdGenerator")
    @GeneratedValue(generator = "versionId")
    private String id;

    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    private int versionNumber;

    @Enumerated(EnumType.STRING)
    private Version version;

    private String productName;

    private String sku;

    private String description;

    private BigDecimal price;

    public ProductVersion(final String productName, final String sku, final String description, final BigDecimal price) {
        this.productName = productName;
        this.sku = sku;
        this.description = description;
        this.price = price;
    }

    @Override
    public Product getParent() {
        return product;
    }

    public ProductVersion copy() {

        final ProductVersion copy = new ProductVersion(productName, sku, description, price);
        copy.setVersionNumber(versionNumber + 1);
        copy.setVersion(Version.DESIGN);

        return copy;
    }
}
