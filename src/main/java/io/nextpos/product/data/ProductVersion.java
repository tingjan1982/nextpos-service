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

    /**
     * Used in order display and working area output for easy recognition.
     */
    private String internalProductName;

    private String sku;

    private String description;

    private BigDecimal price;

    private BigDecimal costPrice;

    @Embedded
    private ProductImage productImage;
    

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

    public void updateProductImage(byte[] imageBinary) {
        this.productImage = new ProductImage(imageBinary);
    }

    public void deleteProductImage() {
        this.productImage = null;
    }

    public ProductVersion copy() {

        final ProductVersion copy = new ProductVersion(productName, sku, description, price);
        copy.setVersionNumber(versionNumber + 1);
        copy.setVersion(Version.DESIGN);
        copy.setInternalProductName(internalProductName);
        copy.setCostPrice(costPrice);

        return copy;
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImage {

        /**
         * Handles this: org.hsqldb.HsqlException: data exception: string data, right truncation
         * https://stackoverflow.com/questions/7565280/hsqlexception-data-exception
         */
        @Lob
        @Column(length = 5 * 1024 * 1000)
        @Basic(fetch = FetchType.LAZY)
        private byte[] binaryData;
    }
}
