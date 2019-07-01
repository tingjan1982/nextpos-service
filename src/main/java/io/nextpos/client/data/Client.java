package io.nextpos.client.data;

import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductOption;
import io.nextpos.shared.model.BaseObject;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;

/**
 * The 1 to many associations here are declared in case of a force deletion of client that
 * would also cascade deletions of associated objects.
 */
@Entity(name = "client")
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

    private Status status = Status.ACTIVE;

    @OneToMany(mappedBy = "client", cascade = CascadeType.REMOVE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Product> products;

    @OneToMany(mappedBy = "client", cascade = CascadeType.REMOVE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductOption> productOptions;


    public Client(final String clientName, final String username, final String masterPassword) {
        this.clientName = clientName;
        this.username = username;
        this.masterPassword = masterPassword;
    }

    public enum Status {
        ACTIVE, INACTIVE, DELETED
    }
}
