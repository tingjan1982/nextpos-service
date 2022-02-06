package io.nextpos.script;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.client.service.ClientService;
import io.nextpos.reservation.data.Reservation;
import io.nextpos.reservation.service.ReservationService;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Disabled
@SpringBootTest
@ActiveProfiles("gcp")
@TestPropertySource(properties = {"script=true", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"})
public class ManageClientReservation {

    private final ReservationService reservationService;

    private final ClientService clientService;

    @Autowired
    public ManageClientReservation(ReservationService reservationService, ClientService clientService) {
        this.reservationService = reservationService;
        this.clientService = clientService;
    }

    @Test
    void findReservations() {

        clientService.getClientByUsername("ron@gmail.com").ifPresent(c -> {

            LocalDateTime startDt = LocalDateTime.of(2022, 1, 14, 18, 0);
            LocalDateTime endDt = LocalDateTime.of(2022, 1, 14, 22, 0);
            final Date reservationStartDate = DateTimeUtil.toDate(c.getZoneId(), startDt);
            final Date reservationEndDate = DateTimeUtil.toDate(c.getZoneId(), endDt);
            List<Reservation> reservations = reservationService.getReservationsByDateRange(c, reservationStartDate, reservationEndDate, List.of());

            System.out.println("Reservation count: " + reservations.size());

            reservations.forEach(r -> {
                System.out.printf("%s (%s) - %s\n", r.getName(), r.getStatus(), DateTimeUtil.toLocalDateTime(c.getZoneId(), r.getStartDate()));
            });
        });
    }

}
