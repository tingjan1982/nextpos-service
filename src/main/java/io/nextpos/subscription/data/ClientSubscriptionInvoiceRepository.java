package io.nextpos.subscription.data;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface ClientSubscriptionInvoiceRepository extends MongoRepository<ClientSubscriptionInvoice, String> {

    List<ClientSubscriptionInvoice> findAllByValidToBetweenAndStatus(Date from, Date to, ClientSubscriptionInvoice.SubscriptionInvoiceStatus invoiceStatus);

    List<ClientSubscriptionInvoice> findAllByDueDateBeforeAndStatus(Date date, ClientSubscriptionInvoice.SubscriptionInvoiceStatus status);

    ClientSubscriptionInvoice findFirstByClientSubscriptionAndStatusOrderByCreatedDateDesc(ClientSubscription clientSubscription, ClientSubscriptionInvoice.SubscriptionInvoiceStatus status);

}
