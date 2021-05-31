package io.nextpos.reservation.data;

import io.nextpos.membership.data.Membership;
import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import io.nextpos.tablelayout.data.TableLayout;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Reservation extends MongoBaseObject implements WithClientId {

    @Id
    private String id;

    private String clientId;

    private ReservationType reservationType;

    /**
     * Booking date
     */
    private Date startDate;

    private Date endDate;

    private String name;

    private String phoneNumber;

    private int people;

    private int kid;

    private List<TableAllocation> tableAllocations = new ArrayList<>();

    /**
     * e.g. baby seat, vegetarian, birthday etc.
     */
    private String note;

    private ReservationStatus status;

    @DBRef
    private Membership membership;

    private Reservation(String clientId, ReservationType reservationType, Date startDate) {
        this.id = new ObjectId().toString();
        this.clientId = clientId;
        this.reservationType = reservationType;
        this.startDate = startDate;
        this.status = ReservationStatus.BOOKED;
    }

    public static Reservation normalReservation(String clientId, Date reservationDate, List<TableLayout.TableDetails> tables) {
        final Reservation reservation = new Reservation(clientId, ReservationType.RESERVATION, reservationDate);
        reservation.updateTableAllocation(tables);

        return reservation;
    }

    public static Reservation waitingReservation(String clientId, Date reservationDate) {
        return new Reservation(clientId, ReservationType.WAITING, reservationDate);
    }

    public void updateTableAllocation(List<TableLayout.TableDetails> tables) {
        this.tableAllocations = tables.stream()
                .map(TableAllocation::new)
                .collect(Collectors.toList());
    }

    public void updateBookingDetails(String name, String phoneNumber, int people, int kid) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.people = people;
        this.kid = kid;
    }

    public enum ReservationType {

        RESERVATION, WAITING
    }


    @Data
    @NoArgsConstructor
    public static class TableAllocation {

        private String tableId;

        private String tableName;

        public TableAllocation(TableLayout.TableDetails tableDetails) {
            this.tableId = tableDetails.getId();
            this.tableName = tableDetails.getTableName();
        }
    }

    public enum ReservationStatus {

        BOOKED,

        /**
         * Booking is confirmed.
         */
        CONFIRMED,

        /**
         * Table is allocated.
         */
        ALLOCATED,

        /**
         * Customer has seated.
         */
        SEATED,

        /**
         * Booking is cancelled.
         */
        CANCELLED,

        DELETED
    }
}
