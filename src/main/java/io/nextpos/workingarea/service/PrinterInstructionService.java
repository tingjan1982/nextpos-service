package io.nextpos.workingarea.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.workingarea.data.PrinterInstructions;

/**
 * Responsible for generating print instructions for various order related print documents.
 */
public interface PrinterInstructionService {

    PrinterInstructions createOrderToWorkingArea(Order order);
}
