package io.nextpos.ordertransaction.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.nextpos.client.data.Client;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.data.OrderTransactionRepository;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.StringWriter;
import java.util.Map;

@Service
@Transactional
public class OrderTransactionServiceImpl implements OrderTransactionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderTransactionServiceImpl.class);

    private final OrderTransactionRepository orderTransactionRepository;

    private final Configuration freeMarkerCfg;

    @Autowired
    public OrderTransactionServiceImpl(final OrderTransactionRepository orderTransactionRepository, final Configuration freeMarkerCfg) {
        this.orderTransactionRepository = orderTransactionRepository;
        this.freeMarkerCfg = freeMarkerCfg;
    }

    @Override
    public OrderTransaction createOrderTransaction(final OrderTransaction orderTransaction) {

        orderTransaction.getPaymentMethodDetails().setPaymentStatus(OrderTransaction.PaymentStatus.SUCCESS);
        final String invoiceNumber = this.getInvoiceNumberExternally();
        orderTransaction.getInvoiceDetails().setInvoiceNumber(invoiceNumber);

        return orderTransactionRepository.save(orderTransaction);
    }

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

    private String getInvoiceNumberExternally() {
        return "DUMMY-E-INVOICE-NUMBER";
    }
}
