package io.nextpos.reservation.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.membership.data.Membership;
import io.nextpos.membership.service.MembershipService;
import io.nextpos.reservation.data.Reservation;
import io.nextpos.reservation.data.ReservationDay;
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
        final LocalDateTime reservationDate = LocalDateTime.of(LocalDate.now(), noonTime);
        final Reservation reservation = Reservation.normalReservation(client.getId(), DateTimeUtil.toDate(client.getZoneId(), reservationDate), tableLayout.getTables());

        reservationService.saveReservation(client, reservation);

        assertThat(reservation.getReservationType()).isEqualByComparingTo(Reservation.ReservationType.RESERVATION);
        assertThat(reservation.getReservationDate()).isNotNull();
        assertThat(reservation.getTableAllocations()).hasSize(2);

        final ReservationDay reservationDay = reservation.getCurrentReservationDay();
        assertThat(reservationDay).isNotNull();

        assertThat(reservationDay).satisfies(day -> {
            assertThat(day.getDate()).isEqualTo(LocalDate.now());
            assertThat(day.getTimeSlots()).hasSize(1);
            assertThat(day.getTimeSlots().get(noonTime)).satisfies(ts -> {
                assertThat(ts).isNotNull();
                assertThat(ts.getTableBookings().values()).allSatisfy(tb -> {
                    assertThat(tb.getReservation()).isEqualTo(reservation);
                    assertThat(tb.getStatus()).isEqualByComparingTo(ReservationDay.TableBooking.BookingStatus.BOOKED);
                });
            });
        });

        reservation.updateTableAllocation(List.of(tableLayout.getTables().get(0)));
        final LocalDateTime newReservationDate = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(13, 0));
        reservation.setReservationDate(DateTimeUtil.toDate(client.getZoneId(), newReservationDate));
        reservation.setMembership(membership);

        reservationService.saveReservation(client, reservation);

        reservationDay.getTimeSlots().values().stream()
                .flatMap(ts -> ts.getTableBookings().values().stream())
                .forEach(tb -> {
                    assertThat(tb.getReservation()).isNull();
                    assertThat(tb.getStatus()).isEqualByComparingTo(ReservationDay.TableBooking.BookingStatus.AVAILABLE);
                });

        assertThat(reservation.getCurrentReservationDay()).isNotEqualTo(reservationDay);

        assertThat(reservationService.getReservation(reservation.getId())).satisfies(r -> {
            assertThat(r.getTableAllocations()).hasSize(1);
            assertThat(r.getMembership()).isNotNull();
        });
    }
}