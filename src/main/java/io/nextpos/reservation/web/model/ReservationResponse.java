package io.nextpos.reservation.web.model;

import io.nextpos.reservation.data.Reservation;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ReservationResponse {

    private String id;

    private Date reservationDate;

    private String name;

    private String phoneNumber;

    private int people;

    private int kid;

    private String note;

    private List<Reservation.TableAllocation> tables;

    private Reservation.ReservationStatus status;

    public ReservationResponse(Reservation reservation) {

        this.id = reservation.getId();
        this.reservationDate = reservation.getReservationDate();
        this.name = reservation.getName();
        this.phoneNumber = reservation.getPhoneNumber();
        this.people = reservation.getPeople();
        this.kid = reservation.getKid();
        this.note = reservation.getNote();
        this.tables = reservation.getTableAllocations();
        this.status = reservation.getStatus();
    }
}
