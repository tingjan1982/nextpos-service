package io.nextpos.script;

import io.nextpos.client.service.ClientService;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueueService;
import io.nextpos.ordermanagement.data.*;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.service.ElectronicInvoiceService;
import io.nextpos.ordertransaction.service.OrderTransactionReportService;
import io.nextpos.ordertransaction.service.OrderTransactionService;
import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Disabled
@SpringBootTest
@ActiveProfiles("gcp")
@TestPropertySource(properties = {"script=true", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"})
public class DeletedOrderSearch {

    private final ClientService clientService;

    private final OrderService orderService;

    private final OrderTransactionService orderTransactionService;

    private final ElectronicInvoiceService electronicInvoiceService;

    private final PendingEInvoiceQueueService pendingEInvoiceQueueService;

    private final MongoTemplate mongoTemplate;

    private final OrderTransactionReportService orderTransactionReportService;

    private final ShiftRepository shiftRepository;

    @Autowired
    public DeletedOrderSearch(ClientService clientService, OrderService orderService, OrderTransactionService orderTransactionService, ElectronicInvoiceService electronicInvoiceService, PendingEInvoiceQueueService pendingEInvoiceQueueService, MongoTemplate mongoTemplate, OrderTransactionReportService orderTransactionReportService, ShiftRepository shiftRepository) {
        this.clientService = clientService;
        this.orderService = orderService;
        this.orderTransactionService = orderTransactionService;
        this.electronicInvoiceService = electronicInvoiceService;
        this.pendingEInvoiceQueueService = pendingEInvoiceQueueService;
        this.mongoTemplate = mongoTemplate;
        this.orderTransactionReportService = orderTransactionReportService;
        this.shiftRepository = shiftRepository;
    }

    /**
     * Use this method to fix electronic invoice record.
     */
    @Test
    void fixElectronicInvoiceRecord() {

        final Order fromOrder = orderTransactionService.getOrderByInvoiceNumber("");
        final Order toOrder = orderTransactionService.getOrderByInvoiceNumber("");
        toOrder.addOrderLineItem(fromOrder.getOrderLineItems().get(0));

        assertThat(orderService.saveOrder(toOrder).getOrderTotal()).isEqualByComparingTo("350");

        System.out.println("Updated order: " + toOrder);

        orderTransactionService.getOrderTransactionByOrderId(toOrder.getId()).forEach(ot -> {
            ot.setOrderTotal(toOrder.getOrderTotal());
            ot.setSettleAmount(toOrder.getOrderTotal());
            ot.updateBillingDetails(toOrder, OrderTransaction.BillType.SINGLE);
            orderTransactionService.saveOrderTransaction(ot);

            System.out.println("Updated order transaction: " + ot);
            assertThat(ot.getOrderTotal()).isEqualByComparingTo("350");
            assertThat(ot.getSettleAmount()).isEqualByComparingTo("350");

            ot.getElectronicInvoice().ifPresent(inv -> {
                final ElectronicInvoice electronicInvoice = electronicInvoiceService.updateElectronicInvoice(inv, toOrder, ot);

                System.out.println("Updated electronic invoice: " + electronicInvoice);
                assertThat(electronicInvoice.getSalesAmount()).isEqualByComparingTo("350");
                assertThat(electronicInvoice.getTaxAmount()).isEqualByComparingTo("17");
                assertThat(electronicInvoice.getInvoiceItems()).hasSize(1);

                final PendingEInvoiceQueue queue = pendingEInvoiceQueueService.reissueErroredEInvoiceQueue(inv.getInvoiceNumber(), PendingEInvoiceQueue.PendingEInvoiceType.CREATE);

                System.out.println("Updated e-invoice queue: " + queue);
                assertThat(queue.getStatus()).isEqualByComparingTo(PendingEInvoiceQueue.PendingEInvoiceStatus.PENDING);
            });
        });
    }

    /**
     * Use this to delete client account orders and related transactional records.
     */
    @Test
    void deleteClientOrdersAndRelatedRecords() {

        clientService.getClientByUsername("Stancwm@gmail.com").ifPresent(c -> {
            Query query = Query.query(where("clientId").is(c.getId()));
            final List<Order> orders = mongoTemplate.findAllAndRemove(query, Order.class);
            System.out.println("Orders: " + orders.size());

            final List<OrderStateChange> stateChanges = mongoTemplate.findAllAndRemove(query, OrderStateChange.class);
            System.out.println("State changes: " + stateChanges.size());

            final List<OrderTransaction> transactions = mongoTemplate.findAllAndRemove(query, OrderTransaction.class);
            System.out.println("Transactions: " + transactions.size());

            final List<OrderSet> orderSets = mongoTemplate.findAllAndRemove(query, OrderSet.class);
            System.out.println("Order sets: " + orderSets.size());

            final List<ElectronicInvoice> electronicInvoices = mongoTemplate.findAllAndRemove(query, ElectronicInvoice.class);
            System.out.println("E-Invoices: " + electronicInvoices.size());

            for (ElectronicInvoice electronicInvoice : electronicInvoices) {
                Query q = Query.query(where("invoiceNumber").is(electronicInvoice.getInvoiceNumber()));
                final List<PendingEInvoiceQueue> queues = mongoTemplate.findAllAndRemove(q, PendingEInvoiceQueue.class);
                System.out.println("Pending EInvoice Queue: " + queues.size());

                final Query subInvoiceQuery = Query.query(where("electronicInvoice").is(electronicInvoice));
                final ClientSubscriptionInvoice updatedInvoice = mongoTemplate.findAndModify(subInvoiceQuery,
                        new Update().set("electronicInvoice", null),
                        FindAndModifyOptions.options().returnNew(true),
                        ClientSubscriptionInvoice.class);
                System.out.println("Updated sub invoice: " + updatedInvoice);
            }

            final List<Shift> shifts = mongoTemplate.findAllAndRemove(query, Shift.class);
            System.out.println("Shifts: " + shifts.size());
        });
    }

    @Test
    void findShiftDiscrepancy() {

//        shiftRepository.findById("60796236d877ae30c6778209").ifPresent(s -> {
//            final ClosingShiftTransactionReport closingShiftTransactionReport = orderTransactionReportService.getClosingShiftTransactionReport(s);
//            System.out.println(closingShiftTransactionReport);
//
//            closingShiftTransactionReport.getEntries().stream()
//                    .map(o -> (Map) o)
//                    .filter(o -> StringUtils.equals((String) o.get("state"), "COMPLETED"))
//                    .map(o -> (String) o.get("orderId"))
//                    .forEach(oid -> {
//                        final Order order = orderService.getOrder(oid);
//                        final BigDecimal txTotal = orderTransactionService.getOrderTransactionByOrderId(oid).stream()
//                                .map(OrderTransaction::getSettleAmount)
//                                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//                        if (order.getOrderTotal().compareTo(txTotal) != 0) {
//                            System.out.println("diff order id: " + oid);
//                        }
//                    });
//        });
    }
}
