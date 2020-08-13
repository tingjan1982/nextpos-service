package io.nextpos.workingarea.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.service.annotation.JpaTransaction;
import io.nextpos.workingarea.data.Printer;
import io.nextpos.workingarea.data.PrinterInstructions;
import io.nextpos.workingarea.data.WorkingArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@JpaTransaction
public class PrinterInstructionServiceImpl implements PrinterInstructionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrinterInstructionServiceImpl.class);

    private final WorkingAreaService workingAreaService;

    private final Configuration freeMarkerCfg;

    @Autowired
    public PrinterInstructionServiceImpl(final WorkingAreaService workingAreaService, final Configuration freeMarkerCfg) {
        this.workingAreaService = workingAreaService;
        this.freeMarkerCfg = freeMarkerCfg;
    }

    @Override
    public PrinterInstructions createOrderToWorkingArea(final Order order) {

        final Map<String, List<OrderLineItem>> lineItemsGroupedByWorkingArea = order.getOrderLineItems().stream()
                .filter(oli -> oli.getState() == OrderLineItem.LineItemState.IN_PROCESS)
                .filter(oli -> oli.getWorkingAreaId() != null)
                .collect(Collectors.groupingBy(OrderLineItem::getWorkingAreaId, Collectors.toList()
                ));

        final Map<WorkingArea, PrinterInstructions.PrinterInstruction> instructions = lineItemsGroupedByWorkingArea.entrySet().stream()
                .map(entry -> createPrinterInstruction(order, entry.getKey(), entry.getValue()))
                .collect(Collectors.toMap(PrinterInstructions.PrinterInstruction::getWorkingArea,
                        pi -> pi));

        return new PrinterInstructions(instructions);
    }

    private PrinterInstructions.PrinterInstruction createPrinterInstruction(final Order order, final String workingAreaId, List<OrderLineItem> lineItems) {

        try {
            Template orderToWorkingArea = freeMarkerCfg.getTemplate("orderToWorkingArea.ftl");

            final WorkingArea workingArea = workingAreaService.getWorkingArea(workingAreaId);
            final StringWriter writer = new StringWriter();
            orderToWorkingArea.process(Map.of("order", order, "lineItems", lineItems), writer);

            final String printInstruction = writer.toString();
            final List<String> printerIps = workingArea.getPrinters().stream()
                    .map(Printer::getIpAddress).collect(Collectors.toList());

            return new PrinterInstructions.PrinterInstruction(workingArea,
                    printInstruction,
                    workingArea.getNoOfPrintCopies(),
                    printerIps);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new GeneralApplicationException("Error while generating order details XML template: " + e.getMessage());
        }
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
    public String createElectronicInvoiceXML(Client client, Order order, OrderTransaction orderTransaction) {

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
}
