package io.nextpos.ordertransaction.service;

import io.nextpos.client.data.Client;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.ElectronicInvoiceRepository;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueue;
import io.nextpos.einvoice.common.invoice.PendingEInvoiceQueueService;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRangeService;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.TaxableAmount;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.shared.service.annotation.MongoTransaction;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@MongoTransaction
public class ElectronicInvoiceServiceImpl implements ElectronicInvoiceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElectronicInvoiceServiceImpl.class);

    private final InvoiceNumberRangeService invoiceNumberRangeService;

    private final PendingEInvoiceQueueService pendingEInvoiceQueueService;

    private final ElectronicInvoiceRepository electronicInvoiceRepository;

    @Autowired
    public ElectronicInvoiceServiceImpl(InvoiceNumberRangeService invoiceNumberRangeService, PendingEInvoiceQueueService pendingEInvoiceQueueService, final ElectronicInvoiceRepository electronicInvoiceRepository) {
        this.invoiceNumberRangeService = invoiceNumberRangeService;
        this.pendingEInvoiceQueueService = pendingEInvoiceQueueService;
        this.electronicInvoiceRepository = electronicInvoiceRepository;
    }

    @Override
    public boolean checkElectronicInvoiceEligibility(Client client) {

        final String aesKey = getAESKey(client);
        final String ubn = client.getAttribute(Client.ClientAttributes.UBN.name());

        return StringUtils.isNotBlank(aesKey) && invoiceNumberRangeService.getCurrentInvoiceNumberRange(ubn) != null;
    }

    @Override
    public ElectronicInvoice createElectronicInvoice(final Client client, final Order order, final OrderTransaction orderTransaction) {

        final String ubn = client.getAttribute(Client.ClientAttributes.UBN.name());
        final String invoiceNumber = invoiceNumberRangeService.resolveInvoiceNumber(ubn);

        final TaxableAmount salesAmount = new TaxableAmount(order.getOrderSettings().getTaxRate(), true);
        salesAmount.calculate(orderTransaction.getSettleAmount());

        final List<ElectronicInvoice.InvoiceItem> items = orderTransaction.getBillDetails().getBillLineItems().stream()
                .map(li -> new ElectronicInvoice.InvoiceItem(li.getName(), li.getQuantity(), li.getUnitPrice(), li.getSubTotal()))
                .collect(Collectors.toList());

        final ElectronicInvoice electronicInvoice = new ElectronicInvoice(
                order.getId(),
                invoiceNumber,
                new ElectronicInvoice.InvoicePeriod(client.getZoneId()),
                salesAmount.getAmountWithTax(),
                salesAmount.getTax(),
                ubn,
                client.getClientName(),
                items);

        if (StringUtils.isNotBlank(orderTransaction.getInvoiceDetails().getTaxIdNumber())) {
            electronicInvoice.setBuyerUbn(orderTransaction.getInvoiceDetails().getTaxIdNumber());
        }

        final String aesKey = getAESKey(client);
        electronicInvoice.generateCodeContent(aesKey);

        final ElectronicInvoice createdElectronicInvoice = electronicInvoiceRepository.save(electronicInvoice);

        pendingEInvoiceQueueService.createPendingEInvoiceQueue(createdElectronicInvoice, PendingEInvoiceQueue.PendingEInvoiceType.CREATE);

        return createdElectronicInvoice;
    }

    @Override
    public void voidElectronicInvoice(ElectronicInvoice electronicInvoice) {

        pendingEInvoiceQueueService.createPendingEInvoiceQueue(electronicInvoice, PendingEInvoiceQueue.PendingEInvoiceType.VOID);
    }

    private String getAESKey(Client client) {
        return client.getAttribute(Client.ClientAttributes.AES_KEY.name());
    }
}
