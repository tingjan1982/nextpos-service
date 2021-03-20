package io.nextpos.inventorymanagement.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class SupplierRequest {

    @NotBlank
    private String supplierName;

    private String contactPerson;

    private String contactEmail;

    private String contactNumber;

    private String contactAddress;
}
