package io.nextpos.invoicenumber.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InvoiceNumberRangesResponse {

    private List<InvoiceNumberRangeResponse> results;
}
