package io.nextpos.reservation.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import io.nextpos.tablelayout.data.TableLayout;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Document
@CompoundIndexes({@CompoundIndex(name = "unique_client_date_index", def = "{'clientId': 1, 'date': 1}", unique = true)})
@Data
@EqualsAndHashCode(callSuper = true)
public class ReservationDay extends MongoBaseObject implements WithClientId {

    @Id
    private String id;

    private String clientId;

    private LocalDate date;

    private Map<LocalTime, TimeSlot> timeSlots = new HashMap<>();

    /**
     * Indicates the reservation is available for the given day.
     */
    private boolean reservable = true;

    public ReservationDay(String clientId, LocalDate date) {
        this.clientId = clientId;
        this.date = date;
    }

    public void allocateTimeSlot(LocalTime timeSlot, List<TableLayout.TableDetails> tables) {
        timeSlots.putIfAbsent(timeSlot, new TimeSlot(timeSlot, tables));
    }

    public void addReservation(Reservation reservation, LocalTime timeSlot) {

        final TimeSlot slot = timeSlots.get(timeSlot);

        reservation.getTableAllocations().forEach(alloc -> {
            final TableBooking tableBooking = slot.getTableBookings().get(alloc.getTableId());
            tableBooking.addReservation(reservation);
        });

        reservation.setCurrentReservationDay(this);
    }

    public void cancelReservation(Reservation reservation) {

        timeSlots.values().stream()
                .flatMap(ts -> ts.getTableBookings().values().stream())
                .forEach(tb -> {
                    if (reservation.equals(tb.getReservation())) {
                        tb.removeReservation();
                    }
                });
    }

    @Data
    @NoArgsConstructor
    public static class TimeSlot {

        private LocalTime time;

        private Map<String, TableBooking> tableBookings;

        public TimeSlot(LocalTime time, List<TableLayout.TableDetails> allTables) {
            this.time = time;
            this.tableBookings = allTables.stream()
                    .map(t -> new TableBooking(t.getId()))
                    .collect(Collectors.toMap(TableBooking::getTableId, t -> t));
        }
    }

    @Data
    @NoArgsConstructor
    public static class TableBooking {

        private String tableId;

        private BookingStatus status;

        @DBRef
        @EqualsAndHashCode.Exclude
        @ToString.Exclude
        private Reservation reservation;

        public TableBooking(String tableId) {
            this.tableId = tableId;
            this.status = BookingStatus.AVAILABLE;
        }

        public void addReservation(Reservation reservation) {
            this.reservation = reservation;
            this.status = BookingStatus.BOOKED;
        }

        public void removeReservation() {
            this.reservation = null;
            this.status = BookingStatus.AVAILABLE;
        }

        public enum BookingStatus {

            AVAILABLE, BOOKED
        }
    }
}
