package io.nextpos.reservation.data;

import io.nextpos.membership.data.Membership;
import io.nextpos.shared.exception.BusinessLogicException;
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
import java.util.Comparator;
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

    private SourceOfOrigin sourceOfOrigin = SourceOfOrigin.APP;

    private Date messageSentDate;

    @DBRef
    private Membership membership;

    private Reservation(String clientId, Date startDate, SourceOfOrigin sourceOfOrigin, List<TableLayout.TableDetails> tables) {
        this.id = new ObjectId().toString();
        this.clientId = clientId;
        this.startDate = startDate;
        this.status = ReservationStatus.WAITING;
        this.sourceOfOrigin = sourceOfOrigin;

        this.updateTableAllocationAndStatus(tables);
    }

    public static Reservation newReservation(String clientId, Date reservationDate, SourceOfOrigin sourceOfOrigin, List<TableLayout.TableDetails> tables) {
        return new Reservation(clientId, reservationDate, sourceOfOrigin, tables);
    }

    public void updateTableAllocationAndStatus(List<TableLayout.TableDetails> tables) {
        this.tableAllocations = tables.stream()
                .map(TableAllocation::new)
                .collect(Collectors.toList());

        if (this.tableAllocations.isEmpty() && this.status != ReservationStatus.WAITING) {
            throw new BusinessLogicException("Reservation not in WAITING state cannot have empty tables");
        }

        if (this.status == ReservationStatus.WAITING) {
            this.status = this.tableAllocations.isEmpty() ? ReservationStatus.WAITING : ReservationStatus.BOOKED;
        }
    }

    public void updateBookingDetails(String name, String phoneNumber, int people, int kid) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.people = people;
        this.kid = kid;
    }

    public static Comparator<Reservation> getComparator() {
        return Comparator.comparing(Reservation::getStatus,
                Comparator.comparing(Reservation.ReservationStatus::getOrdering));
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

        /**
         * No table assigned.
         */
        WAITING(0),

        /**
         * Same as WAITING, confirmed by customer.
         */
        WAITING_CONFIRMED(0),

        /**
         * Secured a table for the reservation.
         */
        BOOKED(0),

        /**
         * Booking is confirmed.
         */
        CONFIRMED(0),

        /**
         * Customer has seated.
         */
        SEATED(0),

        /**
         * Booking is cancelled.
         */
        CANCELLED(1),

        DELETED(1);

        private final Integer ordering;

        ReservationStatus(int ordering) {
            this.ordering = ordering;
        }

        public Integer getOrdering() {
            return ordering;
        }
    }

    public enum SourceOfOrigin {

        APP, WEB
    }
}
