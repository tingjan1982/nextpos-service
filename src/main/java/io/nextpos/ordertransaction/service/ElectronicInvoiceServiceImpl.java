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
import io.nextpos.shared.exception.ObjectNotFoundException;
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

        return StringUtils.isNotBlank(aesKey) && invoiceNumberRangeService.hasCurrentInvoiceNumberRange(ubn);
    }

    @Override
    public ElectronicInvoice createElectronicInvoice(final Client client, final Order order, final OrderTransaction orderTransaction) {

        final String ubn = client.getAttribute(Client.ClientAttributes.UBN);
        final String companyName = client.getAttribute(Client.ClientAttributes.COMPANY_NAME);
        final String invoiceNumber = getInvoiceNumber(ubn);

        final TaxableAmount salesAmount = new TaxableAmount(order.getOrderSettings().getTaxRate(), true);
        salesAmount.calculate(orderTransaction.getSettleAmount());

        final List<ElectronicInvoice.InvoiceItem> items = orderTransaction.getBillDetails().getBillLineItems().stream()
                .map(li -> new ElectronicInvoice.InvoiceItem(li.getName(), li.getQuantity(), li.getUnitPrice(), li.getSubTotal()))
                .collect(Collectors.toList());

        ElectronicInvoice.InvoiceStatus invoiceStatus = !invoiceNumber.equals(INVOICE_NUMBER_MISSING) ? ElectronicInvoice.InvoiceStatus.CREATED : ElectronicInvoice.InvoiceStatus.INVOICE_NUMBER_MISSING;
        final ElectronicInvoice electronicInvoice = new ElectronicInvoice(
                order.getId(),
                invoiceNumber,
                invoiceStatus,
                new ElectronicInvoice.InvoicePeriod(client.getZoneId()),
                salesAmount.getAmountWithTax(),
                salesAmount.getTax(),
                ubn,
                companyName,
                items);

        orderTransaction.updateElectronicInvoiceOptionalDetails(electronicInvoice);

        final String aesKey = getAESKey(client);
        electronicInvoice.generateCodeContent(aesKey);

        final ElectronicInvoice createdElectronicInvoice = electronicInvoiceRepository.save(electronicInvoice);

        PendingEInvoiceQueue.PendingEInvoiceType type = invoiceNumber.equals(INVOICE_NUMBER_MISSING) ? PendingEInvoiceQueue.PendingEInvoiceType.INVOICE_NUMBER_MISSING : PendingEInvoiceQueue.PendingEInvoiceType.CREATE;
        pendingEInvoiceQueueService.createPendingEInvoiceQueue(createdElectronicInvoice, type);

        return createdElectronicInvoice;
    }

    private String getInvoiceNumber(String ubn) {
        try {
            return invoiceNumberRangeService.resolveInvoiceNumber(ubn);

        } catch (Exception e) {
            LOGGER.warn("Error while obtaining invoice number for ubn [{}]: {}", ubn, e.getMessage());

            return INVOICE_NUMBER_MISSING;
        }
    }

    @Override
    public ElectronicInvoice getElectronicInvoiceByInvoiceNumber(String internalInvoiceNumber) {
        return electronicInvoiceRepository.findByInternalInvoiceNumber(internalInvoiceNumber).orElseThrow(() -> {
            throw new ObjectNotFoundException(internalInvoiceNumber, ElectronicInvoice.class);
        });
    }

    @Override
    public void cancelElectronicInvoice(ElectronicInvoice electronicInvoice) {

        pendingEInvoiceQueueService.createPendingEInvoiceQueue(electronicInvoice, PendingEInvoiceQueue.PendingEInvoiceType.CANCEL);
    }

    @Override
    public void voidElectronicInvoice(ElectronicInvoice electronicInvoice) {

        pendingEInvoiceQueueService.createPendingEInvoiceQueue(electronicInvoice, PendingEInvoiceQueue.PendingEInvoiceType.VOID);
    }

    private String getAESKey(Client client) {
        return client.getAttribute(Client.ClientAttributes.AES_KEY.name());
    }
}
