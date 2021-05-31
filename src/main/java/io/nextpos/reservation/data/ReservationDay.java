package io.nextpos.reservation.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document
@CompoundIndexes({@CompoundIndex(name = "unique_client_date_index", def = "{'clientId': 1, 'date': 1}", unique = true)})
@Data
@EqualsAndHashCode(callSuper = true)
public class ReservationDay extends MongoBaseObject implements WithClientId {

    @Id
    private String id;

    private String clientId;

    private LocalDate date;

    /**
     * Toggle indicating if reservation is available for the given day.
     */
    private boolean reservable = true;

    public ReservationDay(String clientId, LocalDate date) {
        this.clientId = clientId;
        this.date = date;
    }
}
