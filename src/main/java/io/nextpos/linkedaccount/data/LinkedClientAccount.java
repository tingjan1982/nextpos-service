package io.nextpos.linkedaccount.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.BaseObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class LinkedClientAccount extends BaseObject {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne
    private Client sourceClient;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    private List<Client> linkedClients = new ArrayList<>();

    private boolean sharedUsers;

    private boolean exportProducts;

    private boolean exportOffers;

    public LinkedClientAccount(Client sourceClient) {
        this.sourceClient = sourceClient;
    }

    public void addLinkedClient(Client linkedClient) {

        if (!linkedClients.contains(linkedClient)) {
            linkedClients.add(linkedClient);
        }
    }

    public void removeLinkedClient(Client linkedClientToRemove) {
        linkedClients.remove(linkedClientToRemove);
    }
}
