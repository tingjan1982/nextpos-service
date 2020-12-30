package io.nextpos.inventorymanagement.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class CreateInventoryRequest {

    @NotBlank
    private String sku;
}
