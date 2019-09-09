package io.nextpos.membership.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@CompoundIndexes({@CompoundIndex(name = "unique_per_client_index", def = "{'clientId': 1, 'mobileNumber': 1}")})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Membership extends MongoBaseObject {

    @Id
    private String id;

    private String clientId;

    private String name;

    private String mobileNumber;

    private MembershipStatus membershipStatus;


    public Membership(final String clientId, final String name, final String mobileNumber) {
        this.clientId = clientId;
        this.name = name;
        this.mobileNumber = mobileNumber;
        this.membershipStatus = MembershipStatus.ACTIVE;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public enum MembershipStatus {

        ACTIVE, INACTIVE
    }
}
