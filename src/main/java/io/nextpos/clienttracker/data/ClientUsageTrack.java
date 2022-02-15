package io.nextpos.clienttracker.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.BaseObject;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ClientUsageTrack extends BaseObject {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Client client;

    @Enumerated(EnumType.STRING)
    private TrackingType trackingType;

    private String value;

    private Date activeStamp;

    public ClientUsageTrack(Client client, TrackingType trackingType, String value) {
        this.client = client;
        this.trackingType = trackingType;
        this.value = value;
    }

    public enum TrackingType {

        USER, DEVICE
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientUsageTrack that = (ClientUsageTrack) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
