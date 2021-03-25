package io.nextpos.inventorymanagement.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InventorOrdersResponse {

    private List<InventoryOrderResponse> results;
}
