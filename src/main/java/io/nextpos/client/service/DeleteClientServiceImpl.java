package io.nextpos.client.service;

import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
@ChainedTransaction
public class DeleteClientServiceImpl implements DeleteClientService {

    private final ClientService clientService;

    private final MongoTemplate mongoTemplate;

    public DeleteClientServiceImpl(ClientService clientService, MongoTemplate mongoTemplate) {
        this.clientService = clientService;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void deleteClient(String clientId) {

        deleteClientDocuments(clientId);
        clientService.deleteClient(clientId);
    }

    private void deleteClientDocuments(String clientId) {

        MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = mongoTemplate.getConverter().getMappingContext();
        AtomicInteger totalDocument = new AtomicInteger();
        AtomicInteger documentWithClientId = new AtomicInteger();

        // consider only entities that are annotated with @Document
        mappingContext.getPersistentEntities().stream()
                .filter(it -> it.isAnnotationPresent(Document.class))
                .forEach(it -> {
                    totalDocument.incrementAndGet();
                    final MongoPersistentProperty property = it.getPersistentProperty("clientId");
                    System.out.printf("%s - ", it.getName());

                    if (property != null) {
                        documentWithClientId.incrementAndGet();
                        Query query = Query.query(where("clientId").is(clientId));
                        final List<?> documentsMatchingClient = mongoTemplate.findAllAndRemove(query, it.getType());
                        System.out.printf("found total records %d, deleting...\n", documentsMatchingClient.size());
                    } else {
                        System.out.println("no clientId association");
                    }
                });

        System.out.printf("Processed %d client documents", totalDocument.get());
    }
}
