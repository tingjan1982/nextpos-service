package io.nextpos.invoicenumber.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ElectronicInvoicesResponse {

    private List<ElectronicInvoiceResponse> results;
}
