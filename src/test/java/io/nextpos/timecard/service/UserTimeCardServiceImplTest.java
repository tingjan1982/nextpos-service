package io.nextpos.timecard.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.DummyObjects;
import io.nextpos.timecard.data.UserTimeCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserTimeCardServiceImplTest {

    @Autowired
    private UserTimeCardService userTimeCardService;

    @Autowired
    private ClientService clientService;

    @BeforeEach
    void prepare() {
        final ClientUser clientUser = DummyObjects.dummyClientUser();
        clientService.createClientUser(clientUser);
    }

    @Test
    void clockInAndOut() {

        final Client client = DummyObjects.dummyClient();
        
        final UserTimeCard userTimeCard = userTimeCardService.clockIn(client);

        assertThat(userTimeCard.getId()).isNotNull();
        assertThat(userTimeCard.getClockIn()).isNotNull().isBefore(new Date());
        assertThat(userTimeCard.getTimeCardStatus()).isEqualTo(UserTimeCard.TimeCardStatus.ACTIVE);

        userTimeCardService.getActiveTimeCard(client).orElseThrow();

        final UserTimeCard updatedUserTimeCard = userTimeCardService.clockOut(client);

        assertThat(updatedUserTimeCard.getId()).isEqualTo(userTimeCard.getId());
        assertThat(updatedUserTimeCard.getClockOut()).isNotNull().isAfter(updatedUserTimeCard.getClockIn()).isBefore(new Date());
        assertThat(updatedUserTimeCard.getTimeCardStatus()).isEqualTo(UserTimeCard.TimeCardStatus.COMPLETE);
    }
}