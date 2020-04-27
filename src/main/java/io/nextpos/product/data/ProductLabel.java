package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ClientObject;
import io.nextpos.workingarea.data.WorkingArea;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity(name = "client_product_label")
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "clientId"})})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ProductLabel extends BaseObject implements ClientObject {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "clientId")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Client client;

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private ProductLabel parentLabel;

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private WorkingArea workingArea;
    
    private String orderKey;

    /**
     * https://stackoverflow.com/questions/4334970/hibernate-cannot-simultaneously-fetch-multiple-bags
     */
    @OneToMany(mappedBy = "parentLabel", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<ProductLabel> childLabels = new ArrayList<>();

    @OneToMany(mappedBy = "productLabel", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(value = FetchMode.SUBSELECT)
    private List<ProductOptionRelation.ProductOptionOfLabel> productOptionOfLabels = new ArrayList<>();


    public ProductLabel(final String name, final Client client) {
        this.name = name;
        this.client = client;
    }

    public ProductLabel addChildProductLabel(String labelName) {

        final ProductLabel childLabel = new ProductLabel(labelName, client);
        childLabel.setParentLabel(this);

        childLabels.add(childLabel);

        return childLabel;
    }

    /**
     * Remove existing product options by setting parent to null in ProductOptionOfLabel.
     */
    public void replaceProductOptions(ProductOption... productOptions) {

        ProductOptionHelper.replaceProductOptions(productOptionOfLabels,
                this::addProductOption,
                productOptions);
    }

    private ProductOptionRelation.ProductOptionOfLabel addProductOption(ProductOption productOption) {
        return new ProductOptionRelation.ProductOptionOfLabel(productOption, this);
    }

    public static class ProductLabelComparator implements Comparator<ProductLabel> {
        @Override
        public int compare(final ProductLabel o1, final ProductLabel o2) {

            if (o1.getOrderKey() != null && o2.getOrderKey() != null) {
                return o1.getOrderKey().compareTo(o2.getOrderKey());
            }

            return o1.getName().compareTo(o2.getName());
        }
    }
}
