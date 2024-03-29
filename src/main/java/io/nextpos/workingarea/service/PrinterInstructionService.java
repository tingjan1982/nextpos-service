package io.nextpos.workingarea.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordermanagement.data.UpdateTableInfo;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.workingarea.data.PrinterInstructions;
import io.nextpos.workingarea.data.SinglePrintInstruction;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Responsible for generating print instructions for various order related print documents.
 */
public interface PrinterInstructionService {

    PrinterInstructions createOrderToWorkingArea(Order order);

    PrinterInstructions createOrderToWorkingArea(Order order, List<String> lineItemIdsToFilter, boolean bypassStateCheck);

    SinglePrintInstruction createUpdateTableInfoInstruction(Client client, UpdateTableInfo updateTableInfo);

    String createOrderDetailsPrintInstruction(Client client, Order order, OrderTransaction orderTransaction);

    String createCancelOrderPrintInstruction(Client client, Order order, OrderTransaction orderTransaction);

    String createElectronicInvoiceXML(Client client, Order order, OrderTransaction orderTransaction, boolean reprint);

    SinglePrintInstruction createShiftReportPrintInstruction(Client client, Shift shift);

    ResponseEntity<String> outputToPrinter(String printerIp, String contentXML);
}
