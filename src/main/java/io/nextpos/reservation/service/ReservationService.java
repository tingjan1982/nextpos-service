package io.nextpos.reservation.service;

import io.nextpos.client.data.Client;
import io.nextpos.reservation.data.Reservation;
import io.nextpos.reservation.data.ReservationDay;

import java.time.LocalDate;

public interface ReservationService {

    Reservation saveReservation(Client client, Reservation reservation);

    Reservation getReservation(String id);

    void deleteReservation(String id);

    ReservationDay getReservationDay(Client client, LocalDate localDate);
}
