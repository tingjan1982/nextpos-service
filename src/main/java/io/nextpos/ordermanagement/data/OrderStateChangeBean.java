package io.nextpos.ordermanagement.data;

import io.nextpos.workingarea.data.PrinterInstructions;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Optional;

@NoArgsConstructor
public class OrderStateChangeBean {

    @Getter
    private OrderStateChange orderStateChange;

    @Setter
    private PrinterInstructions printerInstructions;

    @Getter
    private Order order;

    public OrderStateChangeBean(final OrderStateChange orderStateChange, Order order) {
        this.orderStateChange = orderStateChange;
        this.order = order;
    }

    public Optional<PrinterInstructions> getPrinterInstructions() {
        return Optional.ofNullable(printerInstructions);
    }
}
