package io.nextpos.script;

import io.nextpos.client.data.ClientUserRepository;
import io.nextpos.client.service.ClientService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

@Disabled
@SpringBootTest
@ActiveProfiles("gcp")
@TestPropertySource(properties = {"script=true", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"})
public class PopulateClientUserId {

    static {
        System.setProperty("jdk.tls.client.protocols", "TLSv1, TLSv1.1, TLSv1.2");
    }

    private final ClientService clientService;

    private final ClientUserRepository clientUserRepository;

    @Autowired
    public PopulateClientUserId(ClientService clientService, ClientUserRepository clientUserRepository) {
        this.clientService = clientService;
        this.clientUserRepository = clientUserRepository;
    }

    @Test
    void populateClientUserId() {

        clientUserRepository.findAll().forEach(cu -> {
            final String id = UUID.randomUUID().toString();
            System.out.printf("username: %s, nickname: %s%n", cu.getUsername(), cu.getNickname());

//            cu.setNewId(id);
//            clientService.saveClientUser(cu);

            System.out.println(cu);
        });

    }
}
