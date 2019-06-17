package io.nextpos.product.data;

import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ObjectVersion;
import io.nextpos.shared.model.VersionableClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.math.BigDecimal;

@Entity(name = "product_version")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ProductVersion extends BaseObject implements ObjectVersion {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    private Integer version;

    private VersionableClientObject.ObjectState state;

    private String productName;

    private String sku;

    private BigDecimal price;

    public ProductVersion(final String productName, final String sku, final BigDecimal price) {
        this.productName = productName;
        this.sku = sku;
        this.price = price;
    }

    @Override
    public Integer getVersion() {
        return version;
    }

    @Override
    public void setVersion(final Integer version) {
        this.version = version;
    }

    @Override
    public VersionableClientObject.ObjectState getState() {
        return state;
    }

    @Override
    public void setState(final VersionableClientObject.ObjectState state) {
        this.state = state;
    }
}
