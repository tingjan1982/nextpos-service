package io.nextpos.membership.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

/**
 * Reference on Mongo compound index:
 * https://www.baeldung.com/spring-data-mongodb-index-annotations-converter*
 * https://docs.mongodb.com/manual/indexes/#unique-indexes
 */
@Document
@CompoundIndexes({@CompoundIndex(name = "unique_per_client_index", def = "{'clientId': 1, 'phoneNumber': 1}", unique = true)})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Membership extends MongoBaseObject implements WithClientId {

    @Id
    private String id;

    private String clientId;

    private String name;

    private Gender gender;

    private LocalDate birthday;

    private String phoneNumber;

    private List<String> tags;

    private MembershipStatus membershipStatus;


    public Membership(final String clientId, final String name, final String phoneNumber) {
        this.clientId = clientId;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.membershipStatus = MembershipStatus.ACTIVE;
    }

    /**
     * Can be used to filter membership search.
     */
    public enum MembershipStatus {

        ACTIVE,

        INACTIVE
    }

    public enum Gender {
        MALE, FEMALE
    }
}
