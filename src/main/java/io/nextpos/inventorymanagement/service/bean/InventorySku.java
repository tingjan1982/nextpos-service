package io.nextpos.inventorymanagement.service.bean;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InventorySku {

    private String inventoryId;

    private String productName;

    private String sku;

    private String skuName;
}
