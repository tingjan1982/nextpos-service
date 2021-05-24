package io.nextpos.workingarea.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordermanagement.data.ShiftReport;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.service.annotation.JpaTransaction;
import io.nextpos.shared.util.ImageCodeUtil;
import io.nextpos.workingarea.data.Printer;
import io.nextpos.workingarea.data.PrinterInstructions;
import io.nextpos.workingarea.data.SinglePrintInstruction;
import io.nextpos.workingarea.data.WorkingArea;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@JpaTransaction
public class PrinterInstructionServiceImpl implements PrinterInstructionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrinterInstructionServiceImpl.class);

    private static final String NO_WORKING_AREA = "noWorkingArea";

    private final WorkingAreaService workingAreaService;

    private final ClientService clientService;

    private final ImageCodeUtil imageCodeUtil;

    private final Configuration freeMarkerCfg;

    @Autowired
    public PrinterInstructionServiceImpl(final WorkingAreaService workingAreaService, ClientService clientService, ImageCodeUtil imageCodeUtil, final Configuration freeMarkerCfg) {
        this.workingAreaService = workingAreaService;
        this.clientService = clientService;
        this.imageCodeUtil = imageCodeUtil;
        this.freeMarkerCfg = freeMarkerCfg;
    }

    @Override
    public PrinterInstructions createOrderToWorkingArea(final Order order) {

        return this.createOrderToWorkingArea(order, List.of(), false);
    }

    /**
     * bypassStateCheck is used for reprinting working orders.
     */
    @Override
    public PrinterInstructions createOrderToWorkingArea(final Order order, List<String> lineItemIdsToFilter, boolean bypassStateCheck) {

        final Client client = clientService.getClientOrThrows(order.getClientId());
        final List<Printer> printers = workingAreaService.getPrinters(client);

        if (CollectionUtils.isEmpty(printers)) {
            LOGGER.warn("No printer is setup for client {}={}", client.getId(), client.getClientName());
            return null;
        }

        final Map<String, List<OrderLineItem>> lineItemsGroupedByWorkingArea = order.getOrderLineItems().stream()
                .filter(oli -> CollectionUtils.isEmpty(lineItemIdsToFilter) || lineItemIdsToFilter.contains(oli.getId()))
                .filter(oli -> bypassStateCheck || oli.getState() == OrderLineItem.LineItemState.IN_PROCESS)
                .collect(Collectors.groupingBy(oli -> StringUtils.isNotBlank(oli.getWorkingAreaId()) ? oli.getWorkingAreaId() : NO_WORKING_AREA,
                        Collectors.toList()
                ));

        final List<PrinterInstructions.PrinterInstruction> instructions = lineItemsGroupedByWorkingArea.entrySet().stream()
                .map(entry -> createPrinterInstruction(client, order, entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());

        return new PrinterInstructions(instructions);
    }

    private PrinterInstructions.PrinterInstruction createPrinterInstruction(Client client, Order order, List<OrderLineItem> lineItems, String workingAreaId) {

        try {
            String templateToUse = determineWorkingOrderTemplate(client);
            Template orderToWorkingArea = freeMarkerCfg.getTemplate(templateToUse);

            final StringWriter writer = new StringWriter();
            orderToWorkingArea.process(Map.of("order", order, "lineItems", lineItems), writer);
            final String printInstruction = writer.toString();

            WorkingArea workingArea = null;
            List<String> printerIps = new ArrayList<>();
            int noOfPrintCopies = 1;

            if (!NO_WORKING_AREA.equals(workingAreaId) && (workingArea = workingAreaService.getWorkingArea(workingAreaId)) != null && !CollectionUtils.isEmpty(workingArea.getPrinters())) {
                workingArea.getPrinters().stream()
                        .map(Printer::getIpAddress)
                        .forEach(printerIps::add);

                noOfPrintCopies = workingArea.getNoOfPrintCopies();
            } else {
                LOGGER.info("Resort to finding default printer with service type of WORKING_AREA, then CHECKOUT.");
                List<Printer> printers = workingAreaService.getPrintersByServiceType(client, Printer.ServiceType.WORKING_AREA);

                if (printers.isEmpty()) {
                    printers = workingAreaService.getPrintersByServiceType(client, Printer.ServiceType.CHECKOUT);
                }

                printers.forEach(printer -> {
                    LOGGER.warn("Found printer {}, ip={}", printer.getName(), printer.getIpAddress());
                    printerIps.add(printer.getIpAddress());
                });
            }

            return new PrinterInstructions.PrinterInstruction(workingArea,
                    printInstruction,
                    noOfPrintCopies,
                    printerIps);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new GeneralApplicationException("Error while generating order details XML template: " + e.getMessage());
        }
    }

    private String determineWorkingOrderTemplate(Client client) {

        final String orderDisplayMode = client.getAttribute(Client.ClientAttributes.ORDER_DISPLAY_MODE);

        return StringUtils.isBlank(orderDisplayMode) || orderDisplayMode.equals("LINE_ITEM") ? "lineItemsToWorkingArea.ftl" : "orderToWorkingArea.ftl";
    }

    @Override
    public String createOrderDetailsPrintInstruction(Client client, Order order, OrderTransaction orderTransaction) {

        final Template orderDetails;
        try {
            orderDetails = freeMarkerCfg.getTemplate("orderDetails.ftl");
            final StringWriter writer = new StringWriter();
            final Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("client", client);
            dataModel.put("order", order);
            
            if (orderTransaction != null) {
                dataModel.put("orderTransaction", orderTransaction);
            }

            orderDetails.process(dataModel, writer);

            return writer.toString();

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new GeneralApplicationException("Error while generating order details XML template: " + e.getMessage());
        }
    }

    @Override
    public String createCancelOrderPrintInstruction(Client client, Order order, OrderTransaction orderTransaction) {

        final Optional<ElectronicInvoice> electronicInvoice = orderTransaction.getElectronicInvoice();

        if (electronicInvoice.isEmpty()) {
            return null;
        }

        final Template orderDetails;
        try {
            orderDetails = freeMarkerCfg.getTemplate("cancelOrder.ftl");
            final StringWriter writer = new StringWriter();
            final Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("client", client);
            dataModel.put("electronicInvoice", electronicInvoice.get());
            dataModel.put("order", order);
            dataModel.put("orderTransaction", orderTransaction);

            orderDetails.process(dataModel, writer);

            return writer.toString();

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new GeneralApplicationException("Error while generating order details XML template: " + e.getMessage());
        }
    }

    @Override
    public String createElectronicInvoiceXML(Client client, Order order, OrderTransaction orderTransaction, boolean reprint) {

        final Optional<ElectronicInvoice> electronicInvoice = orderTransaction.getElectronicInvoice();

        if (electronicInvoice.isEmpty()) {
            return null;
        }

        final Template electronicInvoiceTemplate;

        try {
            electronicInvoiceTemplate = freeMarkerCfg.getTemplate("eInvoice.ftl");
            final StringWriter writer = new StringWriter();
            final ElectronicInvoice eInvoice = electronicInvoice.get();
            final String qrcode1ImageBinary = imageCodeUtil.generateBase64ImageBinary(() -> imageCodeUtil.generateQRCode(eInvoice.getQrCode1Content()));
            eInvoice.setQrCode1ImageBinary(qrcode1ImageBinary);

            final String qrcode2ImageBinary = imageCodeUtil.generateBase64ImageBinary(() -> imageCodeUtil.generateQRCode(eInvoice.getQrCode2Content()));
            eInvoice.setQrCode2ImageBinary(qrcode2ImageBinary);

            electronicInvoiceTemplate.process(Map.of("client", client,
                    "order", order,
                    "orderTransaction", orderTransaction,
                    "electronicInvoice", eInvoice,
                    "reprint", reprint), writer);

            return writer.toString();

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new GeneralApplicationException("Error while generating electronic invoice XML template: " + e.getMessage());
        }
    }

    @Override
    public SinglePrintInstruction createShiftReportPrintInstruction(Client client, Shift shift) {

        final ShiftReport shiftReport = new ShiftReport(client, shift);
        final String printInstruction = renderFreeMarkerContent("shiftReport.ftl", Map.of("shift", shiftReport));
        final List<Printer> printers = workingAreaService.getPrintersByServiceType(client, Printer.ServiceType.CHECKOUT);

        if (CollectionUtils.isEmpty(printers)) {
            throw new BusinessLogicException("message.noPrinter", "No checkout printer is setup");
        }

        return new SinglePrintInstruction(printers.get(0).getIpAddress(), printInstruction);
    }

    private String renderFreeMarkerContent(String templateFile, Map<String, Object> data) {

        try {
            Template template = freeMarkerCfg.getTemplate(templateFile);
            final StringWriter writer = new StringWriter();
            template.process(data, writer);

            return writer.toString();

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new GeneralApplicationException("Error while rendering FreeMarker template: " + e.getMessage(), e);
        }
    }

    @Override
    public ResponseEntity<String> outputToPrinter(String printerIp, String contentXML) {

        final RestTemplate restTemplate = new RestTemplate();

        String url = "http://" + printerIp + "/cgi-bin/epos/service.cgi?devid=local_printer&timeout=30000";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/xml; charset=utf-8"));
        headers.add("SOAPAction", "");

        HttpEntity<String> request = new HttpEntity<>(contentXML, headers);

        final ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        LOGGER.info("Status: {}, body: {}", response.getStatusCode(), response.getBody());

        return response;
    }
}
