package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.BaseObject;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "client_product_label")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ProductLabel extends BaseObject {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    private String name;

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Client client;

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private ProductLabel parentLabel;

    @OneToMany(mappedBy = "parentLabel")
    private List<ProductLabel> childLabels = new ArrayList<>();

    public ProductLabel(final String name, final Client client) {
        this.name = name;
        this.client = client;
    }

    public void addChildProductLabel(String labelName) {

        final ProductLabel childLabel = new ProductLabel(labelName, client);
        childLabel.setParentLabel(this);

        childLabels.add(childLabel);
    }
}
