package io.nextpos.ordertransaction.data;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ElectronicInvoiceRepository extends MongoRepository<ElectronicInvoice, String> {
}
