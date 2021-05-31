package io.nextpos.reservation.web.model;

import io.nextpos.client.data.Client;
import io.nextpos.reservation.data.Reservation;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class ReservationResponse {

    private String id;

    private ClientInfo client;

    private Date reservationStartDate;

    private Date reservationEndDate;

    private String name;

    private String phoneNumber;

    private int people;

    private int kid;

    private String note;

    private List<Reservation.TableAllocation> tables;

    private Reservation.ReservationStatus status;

    public ReservationResponse(Reservation reservation) {
        this(null, reservation);
    }

    public ReservationResponse(Client client, Reservation reservation) {

        this.id = reservation.getId();

        if (client != null) {
            this.client = new ClientInfo(client);
        }

        this.reservationStartDate = reservation.getStartDate();
        this.reservationEndDate = reservation.getEndDate();
        this.name = reservation.getName();
        this.phoneNumber = reservation.getPhoneNumber();
        this.people = reservation.getPeople();
        this.kid = reservation.getKid();
        this.note = reservation.getNote();
        this.tables = reservation.getTableAllocations();
        this.status = reservation.getStatus();
    }

    @Data
    @AllArgsConstructor
    public static class ClientInfo {

        private String clientName;

        private String phoneNumber;

        private String address;

        public ClientInfo(Client client) {
            this.clientName = client.getClientName();
            this.address = client.getAttribute(Client.ClientAttributes.ADDRESS);
        }
    }
}
