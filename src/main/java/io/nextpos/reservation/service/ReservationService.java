package io.nextpos.reservation.service;

import io.nextpos.client.data.Client;
import io.nextpos.reservation.data.Reservation;
import io.nextpos.reservation.data.ReservationDay;
import io.nextpos.tablelayout.data.TableLayout;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Date;
import java.util.List;

public interface ReservationService {

    Reservation saveReservation(Client client, Reservation reservation);

    void sendReservationNotification(Client client, Reservation reservation);

    List<TableLayout.TableDetails> getAvailableReservableTables(Client client, LocalDateTime reservationTime);

    List<TableLayout.TableDetails> getAvailableReservableTables(Client client, LocalDateTime reservationTime, String reservationId);

    Reservation getReservation(String id);

    void confirmReservation(Reservation reservation);

    void seatReservation(Reservation reservation);

    void cancelReservation(Reservation reservation);

    void deleteReservation(String id);

    ReservationDay getReservationDay(Client client, LocalDate localDate);

    List<Reservation> getReservationsByDateRange(Client client, LocalDateTime startDt, LocalDateTime endDt);

    List<Reservation> getReservationsByDateRange(Client client, YearMonth yearMonth);

    List<Reservation> getReservationsByDateRange(Client client, Date startDate, Date endDate, List<Reservation.ReservationStatus> statusToFilter);

    List<Reservation> getReservationsByDateAndStatus(Client client, LocalDate reservationDate, Reservation.ReservationStatus reservationStatus);

    Reservation delayReservation(Client client, Reservation reservation, long minutesToDelay);
}
