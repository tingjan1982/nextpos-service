package io.nextpos.reservation.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.mongodb.core.index.Indexed;
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

    @Indexed(unique = true)
    private String clientId;

    /**
     * This is used a condensed version of looking up a client's web reservation page.
     */
    private String reservationKey;

    private Duration reservationDuration;

    private Period maxReservableTime;

    private List<String> nonReservableTables = new ArrayList<>();

    public ReservationSettings(String clientId) {
        this.clientId = clientId;
        this.reservationKey = RandomStringUtils.randomAlphanumeric(6);
        this.maxReservableTime = Period.ofWeeks(2);
        this.reservationDuration = Duration.ofHours(2);
    }

    public LocalDateTime getEndDate(LocalDateTime reservationTime) {
        return reservationTime.plusMinutes(reservationDuration.toMinutes());
    }
}
