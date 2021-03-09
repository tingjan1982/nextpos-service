package io.nextpos.ordermanagement.data;

import lombok.Data;

import java.util.List;

@Data
public class InProcessOrders {

    private List<InProcessOrder> results;

    private boolean needAlert;

    public InProcessOrders(List<InProcessOrder> results) {
        this.results = results;
    }
}
