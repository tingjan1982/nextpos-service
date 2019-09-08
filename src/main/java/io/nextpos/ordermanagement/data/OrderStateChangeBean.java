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

    public OrderStateChangeBean(final OrderStateChange orderStateChange) {
        this.orderStateChange = orderStateChange;
    }

    public Optional<PrinterInstructions> getPrinterInstructions() {
        return Optional.ofNullable(printerInstructions);
    }
}
