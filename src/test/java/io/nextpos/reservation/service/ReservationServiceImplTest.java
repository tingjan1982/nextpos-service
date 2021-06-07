package io.nextpos.reservation.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.membership.data.Membership;
import io.nextpos.membership.service.MembershipService;
import io.nextpos.reservation.data.Reservation;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class ReservationServiceImplTest {

    private final ReservationService reservationService;

    private final ClientService clientService;

    private final TableLayoutService tableLayoutService;

    private final MembershipService membershipService;

    private Client client;

    private TableLayout tableLayout;

    private Membership membership;

    @Autowired
    ReservationServiceImplTest(ReservationService reservationService, ClientService clientService, TableLayoutService tableLayoutService, MembershipService membershipService) {
        this.reservationService = reservationService;
        this.clientService = clientService;
        this.tableLayoutService = tableLayoutService;
        this.membershipService = membershipService;
    }

    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        clientService.saveClient(client);

        tableLayout = new TableLayout(client, "floor");
        tableLayout.addTableDetails(new TableLayout.TableDetails("a1", 4));
        tableLayout.addTableDetails(new TableLayout.TableDetails("a2", 4));
        tableLayoutService.saveTableLayout(tableLayout);

        membership = new Membership(client.getId(), "Joe", "0988120232");
        membershipService.saveMembership(membership);
    }

    @Test
    void saveReservation() {

        final LocalTime noonTime = LocalTime.of(12, 0);
        final LocalDateTime reservationDt = LocalDateTime.of(LocalDate.now(), noonTime);
        final LocalDateTime endDt = LocalDateTime.of(LocalDate.now(), noonTime.plusHours(2));
        final Date reservationDate = DateTimeUtil.toDate(client.getZoneId(), reservationDt);

        final Reservation reservation = Reservation.newReservation(client.getId(), reservationDate, tableLayout.getTables());
        reservation.updateBookingDetails("Joe", "0988120232", 4, 2);

        assertThat(reservationService.getAvailableReservableTables(client, reservationDt)).hasSize(2);

        reservationService.saveReservation(client, reservation);

        assertThat(reservation.getStatus()).isEqualByComparingTo(Reservation.ReservationStatus.BOOKED);
        assertThat(reservation.getStartDate()).isNotNull();
        assertThat(reservation.getEndDate()).isNotNull();
        assertThat(reservation.getTableAllocations()).hasSize(2);

        assertThat(reservationService.getAvailableReservableTables(client, reservationDt)).isEmpty();

        assertThat(reservationService.getReservationsByDateRange(client, reservationDt, endDt)).hasSize(1); // 12 - 2
        assertThat(reservationService.getReservationsByDateRange(client, reservationDt.minusHours(1), endDt.minusHours(1))).hasSize(1); // 11 - 1
        assertThat(reservationService.getReservationsByDateRange(client, reservationDt.plusHours(1), endDt.plusHours(1))).hasSize(1); // 1 - 3
        assertThat(reservationService.getReservationsByDateRange(client, reservationDt.minusHours(1), endDt.minusHours(2))).isEmpty(); // 11 - 12
        assertThat(reservationService.getReservationsByDateRange(client, reservationDt.plusHours(2), endDt.plusHours(1))).isEmpty(); // 2 - 3

        reservation.updateTableAllocationAndStatus(List.of(tableLayout.getTables().get(0)));
        final LocalDateTime newReservationDate = reservationDt.plusHours(1);
        reservation.setStartDate(DateTimeUtil.toDate(client.getZoneId(), newReservationDate));
        reservation.setMembership(membership);

        reservationService.saveReservation(client, reservation);
        
        assertThat(reservationService.getReservation(reservation.getId())).satisfies(r -> {
            assertThat(r.getStatus()).isEqualByComparingTo(Reservation.ReservationStatus.BOOKED);
            assertThat(r.getTableAllocations()).hasSize(1);
            assertThat(r.getMembership()).isNotNull();
        });

        assertThat(reservationService.getReservationsByDateAndStatus(client, newReservationDate.toLocalDate(), Reservation.ReservationStatus.WAITING)).isEmpty();
        assertThat(reservationService.getReservationsByDateAndStatus(client, newReservationDate.toLocalDate(), Reservation.ReservationStatus.BOOKED)).hasSize(1);
    }
}