package io.nextpos.script;

import io.nextpos.client.service.ClientService;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import io.nextpos.ordermanagement.data.*;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordertransaction.data.ClosingShiftTransactionReport;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.service.OrderTransactionReportService;
import io.nextpos.ordertransaction.service.OrderTransactionService;
import io.nextpos.reporting.data.DateParameterType;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Disabled
@SpringBootTest
@ActiveProfiles("gcp")
@TestPropertySource(properties = {"script=true", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"})
public class DeletedOrderSearch {

    private final ClientService clientService;

    private final OrderService orderService;

    private final OrderTransactionService orderTransactionService;

    private final MongoTemplate mongoTemplate;

    private final OrderTransactionReportService orderTransactionReportService;

    private final ShiftRepository shiftRepository;

    @Autowired
    public DeletedOrderSearch(ClientService clientService, OrderService orderService, OrderTransactionService orderTransactionService, MongoTemplate mongoTemplate, OrderTransactionReportService orderTransactionReportService, ShiftRepository shiftRepository) {
        this.clientService = clientService;
        this.orderService = orderService;
        this.orderTransactionService = orderTransactionService;
        this.mongoTemplate = mongoTemplate;
        this.orderTransactionReportService = orderTransactionReportService;
        this.shiftRepository = shiftRepository;
    }

    @Test
    void deleteClientOrdersAndRelatedRecords() {

        clientService.getClientByUsername("rain.io.app@gmail.com").ifPresent(c -> {
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
                System.out.println(queues.size());
            }

            final List<Shift> shifts = mongoTemplate.findAllAndRemove(query, Shift.class);
            System.out.println("Shifts: " + shifts.size());
        });
    }

    @Test
    void findShiftDiscrepancy() {

        shiftRepository.findById("60796236d877ae30c6778209").ifPresent(s -> {
            final ClosingShiftTransactionReport closingShiftTransactionReport = orderTransactionReportService.getClosingShiftTransactionReport(s);
            System.out.println(closingShiftTransactionReport);

            closingShiftTransactionReport.getEntries().stream()
                    .map(o -> (Map) o)
                    .filter(o -> StringUtils.equals((String) o.get("state"), "COMPLETED"))
                    .map(o -> (String) o.get("orderId"))
                    .forEach(oid -> {
                        final Order order = orderService.getOrder(oid);
                        final BigDecimal txTotal = orderTransactionService.getOrderTransactionByOrderId(oid).stream()
                                .map(OrderTransaction::getSettleAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        if (order.getOrderTotal().compareTo(txTotal) != 0) {
                            System.out.println("diff order id: " + oid);
                        }
                    });
        });
    }

    @Test
    void findCancellableInvoices() {
        clientService.getClient("cli-xKIXJ6p49htr0VU3oTNfX5HpAozD").ifPresent(c -> {
            final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(c, DateParameterType.MONTH).build();
            System.out.println(zonedDateRange);

            orderService.getOrders(c, zonedDateRange).stream()
                    .filter(o -> o.getState().equals(Order.OrderState.DELETED))
                    .peek(o -> System.out.println("deleted order: " + o.getId()))
                    .forEach(o -> {
                        orderTransactionService.getOrderTransactionByOrderId(o.getId()).forEach(ot -> {
                            System.out.println("tx order: " + ot.getOrderId());
                            ot.getElectronicInvoice().ifPresent(e -> {
                                System.out.println(e.getInvoiceNumber());
                            });
                        });
                    });
        });
    }
}
