package io.nextpos.workingarea.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.service.annotation.JpaTransaction;
import io.nextpos.workingarea.data.Printer;
import io.nextpos.workingarea.data.PrinterInstructions;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@JpaTransaction
public class PrinterInstructionServiceImpl implements PrinterInstructionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrinterInstructionServiceImpl.class);

    private static final String NO_WORKING_AREA = "noWorkingArea";

    private final WorkingAreaService workingAreaService;

    private final ClientService clientService;

    private final Configuration freeMarkerCfg;

    @Autowired
    public PrinterInstructionServiceImpl(final WorkingAreaService workingAreaService, ClientService clientService, final Configuration freeMarkerCfg) {
        this.workingAreaService = workingAreaService;
        this.clientService = clientService;
        this.freeMarkerCfg = freeMarkerCfg;
    }

    @Override
    public PrinterInstructions createOrderToWorkingArea(final Order order) {

        final Client client = clientService.getClientOrThrows(order.getClientId());
        final List<Printer> printers = workingAreaService.getPrinters(client);

        if (CollectionUtils.isEmpty(printers)) {
            LOGGER.warn("No printer is setup for client {}={}", client.getId(), client.getClientName());
            return null;
        }

        final Map<String, List<OrderLineItem>> lineItemsGroupedByWorkingArea = order.getOrderLineItems().stream()
                .filter(oli -> oli.getState() == OrderLineItem.LineItemState.IN_PROCESS)
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
            Template orderToWorkingArea = freeMarkerCfg.getTemplate("orderToWorkingArea.ftl");

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
                LOGGER.warn("Order line item is not associated with a valid working area, using default printer.");

                Printer printer = workingAreaService.getPrinterByServiceType(client, Printer.ServiceType.WORKING_AREA);

                if (printer == null) {
                    printer = workingAreaService.getPrinterByServiceType(client, Printer.ServiceType.CHECKOUT);
                }

                LOGGER.warn("Found printer {}, ip={}", printer.getName(), printer.getIpAddress());
                printerIps.add(printer.getIpAddress());
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
    public String createElectronicInvoiceXML(Client client, Order order, OrderTransaction orderTransaction) {

        if (!orderTransaction.hasElectronicInvoice()) {
            return null;
        }

        final Template electronicInvoice;
        try {
            electronicInvoice = freeMarkerCfg.getTemplate("eInvoice.ftl");
            final StringWriter writer = new StringWriter();
            electronicInvoice.process(Map.of("client", client, "order", order, "orderTransaction", orderTransaction, "electronicInvoice", orderTransaction.getInvoiceDetails().getElectronicInvoice()), writer);

            return writer.toString();

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new GeneralApplicationException("Error while generating electronic invoice XML template: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<String> outputToPrinter(String printerIp, String contentXML) {

        final RestTemplate restTemplate = new RestTemplate();

        String url = "http://" + printerIp + "/cgi-bin/epos/service.cgi?devid=local_printer&timeout=5000";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/xml; charset=utf-8"));
        headers.add("SOAPAction", "");

        HttpEntity<String> request = new HttpEntity<>(contentXML, headers);

        final ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        LOGGER.info("Status: {}, body: {}", response.getStatusCode(), response.getBody());

        return response;
    }
}
