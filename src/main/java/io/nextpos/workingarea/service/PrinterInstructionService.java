package io.nextpos.workingarea.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.workingarea.data.PrinterInstructions;

/**
 * Responsible for generating print instructions for various order related print documents.
 */
public interface PrinterInstructionService {

    PrinterInstructions createOrderToWorkingArea(Order order);

    String createOrderDetailsPrintInstruction(Client client, OrderTransaction orderTransaction);

    String createElectronicInvoiceXML(Client client, Order order, OrderTransaction orderTransaction);
}
