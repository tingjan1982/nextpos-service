package io.nextpos.subscription.data;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface ClientSubscriptionInvoiceRepository extends MongoRepository<ClientSubscriptionInvoice, String> {

    List<ClientSubscriptionInvoice> findAllByDueDateBeforeAndStatus(Date date, ClientSubscriptionInvoice.SubscriptionInvoiceStatus status);

    List<ClientSubscriptionInvoice> findAllByClientSubscriptionOrderByValidToDesc(ClientSubscription clientSubscription);

    List<ClientSubscriptionInvoice> findAllByStatusIn(List<ClientSubscriptionInvoice.SubscriptionInvoiceStatus> status);

    ClientSubscriptionInvoice findByInvoiceIdentifier(String invoiceIdentifier);
}
