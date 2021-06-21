package io.nextpos.reservation.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.reservation.data.ReservationSettings;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class ReservationSettingsServiceImplTest {

    private final ReservationSettingsService reservationSettingsService;

    private final ClientService clientService;

    @Autowired
    ReservationSettingsServiceImplTest(ReservationSettingsService reservationSettingsService, ClientService clientService) {
        this.reservationSettingsService = reservationSettingsService;
        this.clientService = clientService;
    }

    @Test
    void test() {

        Client client = DummyObjects.dummyClient();
        clientService.saveClient(client);

        final ReservationSettings reservationSettings = reservationSettingsService.getReservationSettings(client.getId());

        assertThat(reservationSettings).satisfies(rs -> {
            assertThat(rs.getId()).isEqualTo(client.getId());
            assertThat(rs.getReservationDuration()).isEqualByComparingTo(Duration.ofMinutes(120));
        });

        reservationSettings.setReservationDuration(Duration.ofMinutes(100));
        reservationSettingsService.saveReservationSettings(reservationSettings);

        assertThat(reservationSettingsService.getReservationSettings(client.getId())).satisfies(rs -> {
            assertThat(rs.getReservationDuration()).isEqualByComparingTo(Duration.ofMinutes(100));
        });
    }
}