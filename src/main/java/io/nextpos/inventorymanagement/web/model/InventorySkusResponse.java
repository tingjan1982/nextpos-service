package io.nextpos.inventorymanagement.web.model;

import io.nextpos.inventorymanagement.service.bean.InventorySku;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InventorySkusResponse {

    private List<InventorySku> results;
}
