package io.nextpos.invoicenumber.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ElectronicInvoiceEligibility {

    private boolean eligible;

    private boolean enabled;
}
