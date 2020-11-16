package io.nextpos.client.data;

import io.nextpos.shared.model.BaseObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

/**
 * https://www.baeldung.com/jpa-one-to-one
 */
@Entity(name = "client_info")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ClientInfo extends BaseObject {

    @Id
    @Column(name = "client_info_id")
    private String id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "client_info_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Client client;

    private String ownerName;

    private String contactNumber;

    private String contactAddress;

    private String operationStatus;

    private String leadSource;

    private String requirements;
}
