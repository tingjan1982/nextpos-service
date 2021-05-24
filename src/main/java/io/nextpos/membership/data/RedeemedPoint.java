package io.nextpos.membership.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class RedeemedPoint extends MongoBaseObject implements WithClientId {

    @Id
    private String id;

    private String clientId;

    @DBRef
    private Membership membership;

    private int redeemedPoints;
}
