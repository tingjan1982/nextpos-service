package io.nextpos.script;

import io.nextpos.client.data.ClientUserRepository;
import io.nextpos.client.service.ClientService;
import io.nextpos.timecard.data.UserTimeCardRepository;
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
public class UpdateUserTimeCard {

    static {
        System.setProperty("jdk.tls.client.protocols", "TLSv1, TLSv1.1, TLSv1.2");
    }

    private final UserTimeCardRepository userTimeCardRepository;

    private final ClientService clientService;

    private final ClientUserRepository clientUserRepository;

    @Autowired
    public UpdateUserTimeCard(UserTimeCardRepository userTimeCardRepository, ClientService clientService, ClientUserRepository clientUserRepository) {
        this.userTimeCardRepository = userTimeCardRepository;
        this.clientService = clientService;
        this.clientUserRepository = clientUserRepository;
    }

    @Test
    void updateUserTimeCard() {

        final AtomicInteger count = new AtomicInteger();

        userTimeCardRepository.findAll().forEach(tc -> {
            count.incrementAndGet();

            clientService.getClient(tc.getClientId()).ifPresentOrElse(c -> {
                clientUserRepository.findByClientAndUsername(c, tc.getUsername()).ifPresentOrElse(cu -> {
                    if (tc.getUserId() == null) {
                        System.out.print("[noid] ");

                        tc.setUserId(cu.getId());
                        tc.setNickname(cu.getNickname());
                        userTimeCardRepository.save(tc);
                    }

                    System.out.printf("id: %s, username: %s, nickname: %s\n", tc.getUserId(), tc.getUsername(), tc.getNickname());
                }, () -> {
                    userTimeCardRepository.delete(tc);
                    System.out.println("Removed user time card as user is not found: " + tc.getUsername());
                });
                
            }, () -> {
                userTimeCardRepository.delete(tc);
                System.out.println("Removed user time card whose client does not exist: " + tc.getUsername());
            });
        });

        System.out.println("Total records: " + count);
    }
}
