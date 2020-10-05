package io.nextpos.invoicenumber.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InvoiceNumberRangeResponse {

    private String ubn;

    private String rangeIdentifier;

    private List<NumberRangeResponse> numberRanges;

    @Data
    @AllArgsConstructor
    public static class NumberRangeResponse {

        private String prefix;

        private String rangeFrom;

        private String rangeTo;

        private boolean started;

        private boolean finished;

        private int remainingInvoiceNumbers;
    }
}
