package io.nextpos.reservation.web.model;

import io.nextpos.reservation.data.ReservationDay;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ReservationDayResponse {

    private String id;

    private LocalDate date;

    private List<TimeSlotResponse> timeSlots;

    public ReservationDayResponse(ReservationDay reservationDay) {

        this.id = reservationDay.getId();
        this.date = reservationDay.getDate();
        this.timeSlots = reservationDay.getTimeSlots().values().stream()
                .map(TimeSlotResponse::new)
                .collect(Collectors.toList());
    }

    @Data
    public static class TimeSlotResponse {

        private LocalTime time;

        private List<TableBookingResponse> tableBookings;

        public TimeSlotResponse(ReservationDay.TimeSlot timeSlot) {

            this.time = timeSlot.getTime();
            this.tableBookings = timeSlot.getTableBookings().values().stream()
                    .map(TableBookingResponse::new)
                    .collect(Collectors.toList());
        }
    }

    @Data
    public static class TableBookingResponse {

        private String tableId;

        private ReservationDay.TableBooking.BookingStatus status;

        private ReservationResponse reservation;

        public TableBookingResponse(ReservationDay.TableBooking tableBooking) {

            this.tableId = tableBooking.getTableId();
            this.status = tableBooking.getStatus();

            if (tableBooking.getReservation() != null) {
                this.reservation = new ReservationResponse(tableBooking.getReservation());
            }
        }
    }
}
