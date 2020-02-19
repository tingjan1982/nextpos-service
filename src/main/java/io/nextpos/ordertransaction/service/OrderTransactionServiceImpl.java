package io.nextpos.ordertransaction.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.data.OrderTransactionRepository;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class OrderTransactionServiceImpl implements OrderTransactionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderTransactionServiceImpl.class);

    private final OrderTransactionRepository orderTransactionRepository;

    private final OrderService orderService;

    private final Configuration freeMarkerCfg;

    @Autowired
    public OrderTransactionServiceImpl(final OrderTransactionRepository orderTransactionRepository, final OrderService orderService, final Configuration freeMarkerCfg) {
        this.orderTransactionRepository = orderTransactionRepository;
        this.orderService = orderService;
        this.freeMarkerCfg = freeMarkerCfg;
    }

    @Override
    public OrderTransaction createOrderTransaction(final OrderTransaction orderTransaction) {

        final Order order = orderService.getOrder(orderTransaction.getOrderId());

        if (Order.OrderState.finalStates().contains(order.getState())) {
            throw new BusinessLogicException("Cannot create order transaction. The order has been finalized: " + order.getId());
        }

        LOGGER.info("Creating order transaction for order {}.", order.getId());

        // todo: this would change when we connect to external credit card processor
        orderTransaction.getPaymentMethodDetails().setPaymentStatus(OrderTransaction.PaymentStatus.SUCCESS);
        final String invoiceNumber = this.getInvoiceNumberExternally();
        orderTransaction.getInvoiceDetails().setInvoiceNumber(invoiceNumber);

        final OrderTransaction updatedTransaction = orderTransactionRepository.save(orderTransaction);

        final BigDecimal settledAmount = orderTransactionRepository.findAllByOrderId(orderTransaction.getOrderId()).stream()
                .map(OrderTransaction::getSettleAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (order.getOrderTotal().compareTo(settledAmount) == 0) {
            LOGGER.info("Order[{}] with amount {} has been settled, settling the order.", order.getId(), order.getOrderTotal());
            orderService.performOrderAction(orderTransaction.getOrderId(), Order.OrderAction.SETTLE);
        }

        return updatedTransaction;
    }

    // todo: should refactor this to PrinterService
    @Override
    public String createOrderDetailsPrintInstruction(Client client, OrderTransaction orderTransaction) {

        final Template orderDetails;
        try {
            orderDetails = freeMarkerCfg.getTemplate("orderDetails.ftl");
            final StringWriter writer = new StringWriter();
            orderDetails.process(Map.of("client", client, "orderTransaction", orderTransaction), writer);

            return writer.toString();

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new GeneralApplicationException("Error while generating order details XML template: " + e.getMessage());
        }
    }

    @Override
    public OrderTransaction getOrderTransaction(final String id) {

        return orderTransactionRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, OrderTransaction.class);
        });
    }

    @Override
    public List<OrderTransaction> getOrderTransactionByOrderId(String orderId) {
        return orderTransactionRepository.findAllByOrderId(orderId);
    }

    private String getInvoiceNumberExternally() {
        return "DUMMY-E-INVOICE-NUMBER";
    }
}
