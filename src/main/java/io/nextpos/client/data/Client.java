package io.nextpos.client.data;

import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductOption;
import io.nextpos.shared.model.BaseObject;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The 1 to many associations here are declared in case of a force deletion of client that
 * would also cascade deletions of associated objects.
 */
@Entity(name = "client")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "username"))
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Client extends BaseObject {

    @Id
    @GenericGenerator(name = "clientid", strategy = "io.nextpos.shared.model.idgenerator.ClientIdGenerator")
    @GeneratedValue(generator = "clientid")
    private String id;

    private String clientName;

    private String username;

    private String masterPassword;

    private String roles;

    private String countryCode;

    private Status status = Status.ACTIVE;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "attribute_key")
    @Column(name = "attribute_value")
    @CollectionTable(name = "client_attributes", joinColumns = @JoinColumn(name = "client_id"))
    private Map<String, String> attributes = new HashMap<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductOption> productOptions;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Product> products;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductLabel> productLabels;


    public Client(final String clientName, final String username, final String masterPassword, final String countryCode) {
        this.clientName = clientName;
        this.username = username;
        this.masterPassword = masterPassword;
        this.countryCode = countryCode;
    }

    public Client addAttribute(String key, String value) {
        attributes.put(key, value);
        return this;
    }

    public enum Status {
        ACTIVE, INACTIVE, DELETED
    }
}
