package io.nextpos.tablelayout.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TableLayoutsResponse {

    private List<TableLayoutResponse> tableLayouts;
}
