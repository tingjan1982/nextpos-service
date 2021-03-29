package io.nextpos.ordertransaction.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientSettingsService;
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

    private final ClientSettingsService clientSettingsService;

    private final ElectronicInvoiceRepository electronicInvoiceRepository;

    @Autowired
    public ElectronicInvoiceServiceImpl(InvoiceNumberRangeService invoiceNumberRangeService, PendingEInvoiceQueueService pendingEInvoiceQueueService, ClientSettingsService clientSettingsService, final ElectronicInvoiceRepository electronicInvoiceRepository) {
        this.invoiceNumberRangeService = invoiceNumberRangeService;
        this.pendingEInvoiceQueueService = pendingEInvoiceQueueService;
        this.clientSettingsService = clientSettingsService;
        this.electronicInvoiceRepository = electronicInvoiceRepository;
    }

    @Override
    public boolean checkElectronicInvoiceEligibility(Client client) {

        final String aesKey = getAESKey(client);
        final String ubn = client.getAttribute(Client.ClientAttributes.UBN);
        final String companyName = client.getAttribute(Client.ClientAttributes.COMPANY_NAME);
        final String companyAddress = client.getAttribute(Client.ClientAttributes.ADDRESS);
        boolean electronicInvoiceEnabled = true; //clientSettingsService.getClientSettingBooleanValue(client, ClientSetting.SettingName.ELECTRONIC_INVOICE);

        return StringUtils.isNotBlank(aesKey) &&
                StringUtils.isNotBlank(ubn) &&
                StringUtils.isNotBlank(companyName) &&
                StringUtils.isNotBlank(companyAddress) &&
                invoiceNumberRangeService.hasCurrentInvoiceNumberRange(ubn) &&
                electronicInvoiceEnabled;
    }

    @Override
    public ElectronicInvoice createElectronicInvoice(final Client client, final Order order, final OrderTransaction orderTransaction) {

        final String ubn = client.getAttribute(Client.ClientAttributes.UBN);
        final String companyName = client.getAttribute(Client.ClientAttributes.COMPANY_NAME);
        final String address = client.getAttribute(Client.ClientAttributes.ADDRESS);
        final String invoiceNumber = getInvoiceNumber(ubn);

        final TaxableAmount salesAmount = new TaxableAmount(order.getOrderSettings().getTaxRate(), true);
        salesAmount.calculate(orderTransaction.getSettleAmount());

        final List<ElectronicInvoice.InvoiceItem> items = orderTransaction.getBillDetails().getBillLineItems().stream()
                .map(li -> new ElectronicInvoice.InvoiceItem(li.getName(), li.getQuantity(), li.getUnitPrice(), li.getSubTotal()))
                .collect(Collectors.toList());

        ElectronicInvoice.InvoiceStatus invoiceStatus = isValidInvoiceNumber(invoiceNumber) ? ElectronicInvoice.InvoiceStatus.CREATED : ElectronicInvoice.InvoiceStatus.INVOICE_NUMBER_MISSING;
        final ElectronicInvoice electronicInvoice = new ElectronicInvoice(
                client.getId(),
                order.getId(),
                invoiceNumber,
                invoiceStatus,
                new ElectronicInvoice.InvoicePeriod(client.getZoneId()),
                salesAmount.getAmountWithTax(),
                salesAmount.getTax(),
                ubn,
                companyName,
                address,
                items);

        orderTransaction.updateElectronicInvoiceOptionalDetails(electronicInvoice);

        final String aesKey = getAESKey(client);
        electronicInvoice.generateCodeContent(aesKey);

        final ElectronicInvoice createdElectronicInvoice = electronicInvoiceRepository.save(electronicInvoice);

        if (isValidInvoiceNumber(invoiceNumber)) {
            pendingEInvoiceQueueService.createPendingEInvoiceQueue(createdElectronicInvoice, PendingEInvoiceQueue.PendingEInvoiceType.CREATE);
        }

        return createdElectronicInvoice;
    }

    @Override
    public void issueNewInvoiceNumber(ElectronicInvoice electronicInvoice) {

        if (isValidInvoiceNumber(electronicInvoice.getInvoiceNumber())) {
            LOGGER.warn("This electronic invoice already has a valid invoice number {}", electronicInvoice.getInvoiceNumber());
            return;
        }

        final String newInvoiceNumber = getInvoiceNumber(electronicInvoice.getSellerUbn());

        if (isValidInvoiceNumber(newInvoiceNumber)) {
            electronicInvoice.updateInvoiceNumber(newInvoiceNumber);
            electronicInvoice.setInvoiceStatus(ElectronicInvoice.InvoiceStatus.CREATED);
            pendingEInvoiceQueueService.createPendingEInvoiceQueue(electronicInvoice, PendingEInvoiceQueue.PendingEInvoiceType.CREATE);

            electronicInvoiceRepository.save(electronicInvoice);
        }
    }

    @Override
    public ElectronicInvoice getElectronicInvoice(String id) {
        return electronicInvoiceRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, ElectronicInvoice.class);
        });
    }

    private String getInvoiceNumber(String ubn) {
        try {
            return invoiceNumberRangeService.resolveInvoiceNumber(ubn);

        } catch (Exception e) {
            LOGGER.warn("Error while obtaining invoice number for ubn [{}]: {}", ubn, e.getMessage());

            return INVOICE_NUMBER_MISSING;
        }
    }

    private boolean isValidInvoiceNumber(String invoiceNumber) {
        return !invoiceNumber.equals(INVOICE_NUMBER_MISSING);
    }

    @Override
    public ElectronicInvoice getElectronicInvoiceByInvoiceNumber(String internalInvoiceNumber) {
        return electronicInvoiceRepository.findByInternalInvoiceNumber(internalInvoiceNumber).orElseThrow(() -> {
            throw new ObjectNotFoundException(internalInvoiceNumber, ElectronicInvoice.class);
        });
    }

    @Override
    public List<ElectronicInvoice> getElectronicInvoicesByInvoiceStatus(Client client, ElectronicInvoice.InvoiceStatus invoiceStatus) {
        return electronicInvoiceRepository.findAllByClientIdAndInvoiceStatus(client.getId(), invoiceStatus);
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
