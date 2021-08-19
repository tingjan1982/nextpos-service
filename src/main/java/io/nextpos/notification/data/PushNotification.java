package io.nextpos.notification.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class PushNotification extends MongoBaseObject implements WithClientId {

    @Id
    private String id;

    @Indexed(unique = true)
    private String clientId;

    private Set<String> tokens = new HashSet<>();

    public PushNotification(String clientId) {
        this.clientId = clientId;
    }

    public void addToken(String token) {
        this.tokens.add(token);
    }
}
