package io.nextpos.tablelayout.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TableLayoutResponse {

    private String id;

    private String layoutName;

    private int totalTables;

    private int totalCapacity;

    private List<TableDetailsResponse> tables;
}
