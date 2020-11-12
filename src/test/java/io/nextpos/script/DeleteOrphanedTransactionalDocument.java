package io.nextpos.script;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.client.service.ClientService;
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
public class DeleteOrphanedTransactionalDocument {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteOrphanedTransactionalDocument.class);

    private final ClientService clientService;

    private final ClientRepository clientRepository;

    private final MongoTemplate mongoTemplate;

    @Autowired
    public DeleteOrphanedTransactionalDocument(ClientService clientService, ClientRepository clientRepository, MongoTemplate mongoTemplate) {
        this.clientService = clientService;
        this.clientRepository = clientRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Test
    void deleteDocumentsWithoutClientId() {

        final List<String> clientIds = clientRepository.findAll().stream()
                .peek(c -> LOGGER.info("Client[{}] - {}", c.getId(), c.getClientName()))
                .map(Client::getId).collect(Collectors.toList());

        MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
        AtomicInteger totalDocument = new AtomicInteger();
        AtomicInteger documentWithClientId = new AtomicInteger();
        AtomicInteger documentWithoutClientId = new AtomicInteger();

        // consider only entities that are annotated with @Document
        mappingContext.getPersistentEntities()
                .stream()
                .filter(it -> it.isAnnotationPresent(Document.class))
                .forEach(it -> {
                    LOGGER.info("Processing {} collection", it.getName());
                    totalDocument.incrementAndGet();
                    final MongoPersistentProperty property = it.getPersistentProperty("clientId");

                    if (property != null) {
                        documentWithClientId.incrementAndGet();
                        LOGGER.info("{} collection: Client id property: {}", it.getName(), property);
                        Query query = Query.query(where("clientId").nin(clientIds));
                        final List<?> documentsWithoutClient = mongoTemplate.find(query, it.getType());
                        LOGGER.info("{} collection: found {} documents without client", it.getName(), documentsWithoutClient.size());

                    } else {
                        documentWithoutClientId.incrementAndGet();
                        LOGGER.info("Skipping {} collection without client id", it.getName());
                    }
                });

        LOGGER.info("Summary: total document={}, with client id={}, without client id={}", totalDocument.get(), documentWithClientId.get(), documentWithoutClientId.get());
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
}
