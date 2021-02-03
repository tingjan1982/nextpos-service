package io.nextpos.ordermanagement.data;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class InProcessOrderLineItems {

    private Map<String, List<InProcessOrderLineItem>> results;

    private boolean needAlert;

    public InProcessOrderLineItems(Map<String, List<InProcessOrderLineItem>> results) {
        this.results = results;
    }
}
