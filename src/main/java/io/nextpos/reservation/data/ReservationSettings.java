package io.nextpos.reservation.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class ReservationSettings extends MongoBaseObject implements WithClientId {

    @Id
    private String id;

    private Duration reservationDuration;

    private Period maxReservableTime;

    private List<String> nonReservableTables = new ArrayList<>();

    public ReservationSettings(String id) {
        this.id = id;
        this.maxReservableTime = Period.ofWeeks(2);
        this.reservationDuration = Duration.ofHours(2);
    }

    public LocalDateTime getEndDate(LocalDateTime reservationTime) {
        return reservationTime.plusMinutes(reservationDuration.toMinutes());
    }

    @Override
    public String getClientId() {
        return this.id;
    }
}
