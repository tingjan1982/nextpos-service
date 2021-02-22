package io.nextpos.script;

import io.nextpos.calendarevent.data.CalendarEventRepository;
import io.nextpos.client.data.ClientUserRepository;
import io.nextpos.client.service.ClientService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.atomic.AtomicInteger;

@Disabled
@SpringBootTest
@ActiveProfiles("gcp")
@TestPropertySource(properties = {"script=true", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"})
public class UpdateCalendarEvent {

    static {
        System.setProperty("jdk.tls.client.protocols", "TLSv1, TLSv1.1, TLSv1.2");
    }

    private final CalendarEventRepository calendarEventRepository;

    private final ClientService clientService;

    private final ClientUserRepository clientUserRepository;

    @Autowired
    public UpdateCalendarEvent(CalendarEventRepository calendarEventRepository, ClientService clientService, ClientUserRepository clientUserRepository) {
        this.calendarEventRepository = calendarEventRepository;
        this.clientService = clientService;
        this.clientUserRepository = clientUserRepository;
    }

    @Test
    void updateCalendarEvent() {

        final AtomicInteger count = new AtomicInteger();

        calendarEventRepository.findAll().forEach(ce -> {
            count.incrementAndGet();

            System.out.print(ce.getId());

            ce.getEventResources().forEach(er -> {
                System.out.printf(" Resource id: %s, resource name: %s\n", er.getResourceId(), er.getResourceName());
            });
//
//            clientService.getClient(ce.getClientId()).ifPresentOrElse(c -> {
//                clientUserRepository.findByClientAndUsername(c, ce.getUsername()).ifPresentOrElse(cu -> {
//                    if (ce.getUserId() == null) {
//                        System.out.print("[noid] ");
//
//                        ce.setUserId(cu.getId());
//                        ce.setNickname(cu.getNickname());
//                        calendarEventRepository.save(ce);
//                    }
//
//                    System.out.printf("id: %s, username: %s, nickname: %s\n", ce.getUserId(), ce.getUsername(), ce.getNickname());
//                }, () -> {
//                    calendarEventRepository.delete(ce);
//                    System.out.println("Removed user time card as user is not found: " + ce.getUsername());
//                });
//
//            }, () -> {
//                calendarEventRepository.delete(ce);
//                System.out.println("Removed user time card whose client does not exist: " + ce.getUsername());
//            });
        });

        System.out.println("Total records: " + count);
    }
}
