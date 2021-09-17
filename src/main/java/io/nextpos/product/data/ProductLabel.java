package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ClientObject;
import io.nextpos.shared.model.ObjectOrdering;
import io.nextpos.workingarea.data.WorkingArea;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Entity(name = "client_product_label")
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "clientId"})})
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class ProductLabel extends BaseObject implements ClientObject, ObjectOrdering<Integer> {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @EqualsAndHashCode.Include
    private String id;

    @EqualsAndHashCode.Include
    private String name;

    @ManyToOne
    @JoinColumn(name = "clientId")
    @ToString.Exclude
    private Client client;

    @ManyToOne
    @ToString.Exclude
    private ProductLabel parentLabel;

    @ManyToOne
    @ToString.Exclude
    private WorkingArea workingArea;

    private Integer ordering;

    private String color;

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
        this.ordering = 0;
    }

    public static ProductLabel dynamicLabel(Client client, String name) {

        final ProductLabel productLabel = new ProductLabel(name, client);
        productLabel.setId(name);

        return productLabel;
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

    public static class ProductLabelComparator implements Comparator<ProductLabel>, Serializable {
        @Override
        public int compare(final ProductLabel o1, final ProductLabel o2) {

            final Comparator<ProductLabel> chainedComparator = Comparator.comparing(ProductLabel::getOrdering).thenComparing(ProductLabel::getName);

            return chainedComparator.compare(o1, o2);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductLabel that = (ProductLabel) o;
        return Objects.equals(id, that.id) && name.equals(that.name) && client.equals(that.client);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, client);
    }
}
