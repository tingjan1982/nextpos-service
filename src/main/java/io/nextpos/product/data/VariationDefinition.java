package io.nextpos.product.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.BaseObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity(name = "client_variation_definition")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class VariationDefinition extends BaseObject {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    private String variationName;

    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Client client;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "attribute_value")
    @CollectionTable(joinColumns = @JoinColumn(name = "variation_definition_id"))
    private Set<String> attributes = new HashSet<>();

    public VariationDefinition(Client client, String variationName) {
        this.client = client;
        this.variationName = variationName;
    }

    public void addAttributes(List<String> attributes) {
        this.attributes.addAll(attributes);
    }
}
