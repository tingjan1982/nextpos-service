package io.nextpos.workingarea.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.workingarea.data.PrinterInstructions;
import org.springframework.http.ResponseEntity;

/**
 * Responsible for generating print instructions for various order related print documents.
 */
public interface PrinterInstructionService {

    PrinterInstructions createOrderToWorkingArea(Order order);

    PrinterInstructions createOrderToWorkingArea(Order order, boolean bypassStateCheck);

    String createOrderDetailsPrintInstruction(Client client, Order order, OrderTransaction orderTransaction);

    String createCancelOrderPrintInstruction(Client client, Order order, OrderTransaction orderTransaction);

    String createElectronicInvoiceXML(Client client, Order order, OrderTransaction orderTransaction);

    ResponseEntity<String> outputToPrinter(String printerIp, String contentXML);
}
