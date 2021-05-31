package io.nextpos.reservation.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class ReservationSettings extends MongoBaseObject implements WithClientId {

    @Id
    private String clientId;

    private Duration reservationDuration;

    private int nonReservableTableCount;

    private List<String> nonReservableTables = new ArrayList<>();

    public ReservationSettings(String clientId) {
        this.clientId = clientId;
        this.reservationDuration = Duration.ofHours(2);
    }

    public LocalDateTime getEndDate(LocalDateTime reservationTime) {
        return reservationTime.plusHours(reservationDuration.toHours());
    }

    @Override
    public String getId() {
        return this.clientId;
    }
}
