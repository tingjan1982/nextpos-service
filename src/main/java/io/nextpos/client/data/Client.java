package io.nextpos.client.data;

import io.nextpos.product.data.Product;
import io.nextpos.shared.model.BaseObject;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;

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
    private List<Product> products;


    public Client(final String clientName, final String username, final String masterPassword) {
        this.clientName = clientName;
        this.username = username;
        this.masterPassword = masterPassword;
    }

    public enum Status {
        ACTIVE, INACTIVE, DELETED
    }
}