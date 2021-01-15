package io.nextpos.client.data;

import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.model.BaseObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity(name = "client_password_registry")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ClientPasswordRegistry extends BaseObject {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Client client;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "username")
    @Column(name = "password")
    @CollectionTable(name = "client_used_passwords", joinColumns = @JoinColumn(name = "client_id"))
    private Map<String, String> passwords = new HashMap<>();

    public ClientPasswordRegistry(Client client) {
        this.client = client;
    }

    public boolean isPasswordUsed(String password) {
        return passwords.containsValue(password);
    }

    public void addPassword(String username, String password) {
        passwords.put(username, password);
    }

    public void removePassword(String username) {
        passwords.remove(username);
    }

    public String getUserByPassword(String password) {

        return passwords.entrySet().stream()
                .filter(e -> e.getValue().equals(password))
                .map(Map.Entry::getKey)
                .findFirst().orElseThrow(() -> {
                    throw new ObjectNotFoundException("NA", ClientPasswordRegistry.class);
                });
    }
}
