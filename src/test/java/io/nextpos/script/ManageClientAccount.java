package io.nextpos.script;

import io.nextpos.announcement.data.Announcement;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.client.service.ClientService;
import io.nextpos.client.service.DeleteClientService;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Disabled
@SpringBootTest
@ActiveProfiles("gcp")
@TestPropertySource(properties = {"script=true", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"})
public class ManageClientAccount {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManageClientAccount.class);

    private final ClientService clientService;

    private final DeleteClientService deleteClientService;

    private final ClientRepository clientRepository;

    private final MongoTemplate mongoTemplate;

    @Autowired
    public ManageClientAccount(ClientService clientService, DeleteClientService deleteClientService, ClientRepository clientRepository, MongoTemplate mongoTemplate) {
        this.clientService = clientService;
        this.deleteClientService = deleteClientService;
        this.clientRepository = clientRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Test
    void analyzeDocuments() {

        final List<String> clientIds = clientRepository.findAll().stream()
                .map(Client::getId).collect(Collectors.toList());

        System.out.printf("Client count: %d\n", clientIds.size());

        MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
        AtomicInteger totalDocument = new AtomicInteger();
        AtomicInteger documentWithClientId = new AtomicInteger();
        AtomicInteger documentWithoutClientId = new AtomicInteger();

        // consider only entities that are annotated with @Document
        mappingContext.getPersistentEntities()
                .stream()
                .filter(it -> it.isAnnotationPresent(Document.class))
                .forEach(it -> {
                    totalDocument.incrementAndGet();
                    final MongoPersistentProperty property = it.getPersistentProperty("clientId");
                    System.out.printf("%s - ", it.getName());

                    if (property != null) {
                        documentWithClientId.incrementAndGet();
                        Query query = Query.query(where("clientId").nin(clientIds));
                        final List<?> documentsWithoutClient = mongoTemplate.findAllAndRemove(query, it.getType());
                        System.out.printf("total records with invalid client id: %d", documentsWithoutClient.size());

                        if(!documentsWithoutClient.isEmpty()) {
                            System.out.print(" deleting...");
                        }

                        System.out.println();

                    } else {
                        documentWithoutClientId.incrementAndGet();
                        System.out.println("no clientId field");
                    }
                });

        System.out.printf("Summary - total documents: %d , with client id: %d, without client id: %d", totalDocument.get(), documentWithClientId.get(), documentWithoutClientId.get());
    }

    @Test
    void deleteDocumentWithInvalidClientId() {

        Class<?> documentClassToDelete = ClientSubscriptionInvoice.class;
        final List<String> clientIds = clientRepository.findAll().stream()
                .map(Client::getId).collect(Collectors.toList());

        Query query = Query.query(where("clientId").nin(clientIds));
        final List<?> documentsWithoutClient = mongoTemplate.findAllAndRemove(query, documentClassToDelete);

        System.out.printf("Total deleted records: %d", documentsWithoutClient.size());
    }

    @Test
    void checkClientDocuments() {
        MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
        Map<Client, Boolean> clients = new HashMap<>();

        clientRepository.findAll().forEach(c -> {
            final String id = c.getId();
            clients.put(c, Boolean.FALSE);
            LOGGER.info("Client[{}] - {}", id, c.getClientName());

            mappingContext.getPersistentEntities()
                    .stream()
                    .filter(it -> it.isAnnotationPresent(Document.class))
                    .forEach(it -> {
                        Query queryByClientId = Query.query(where("clientId").is(id));
                        final List<?> documents = mongoTemplate.find(queryByClientId, it.getType());

                        if (!documents.isEmpty()) {
                            clients.put(c, Boolean.TRUE);
                        }

                        LOGGER.info("{}: size: {}", it.getName(), documents.size());
                    });

            LOGGER.info("Client has records: {}", clients.get(c));
        });

        clients.forEach((c, b) -> LOGGER.info("Client[{}] {} - has records: {}", c.getId(), c.getClientName(), b));
    }

    /**
     * Find client id and update clientIdToDelete to delete all records of the specified client.
     */
    @Test
    void deleteClient() {

        String clientIdToDelete = "cli-lc3lVS2GF2mvr5jON5BD083JV0QQ";
        deleteClientService.deleteClient(clientIdToDelete);
    }
}
