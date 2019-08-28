package io.nextpos.tablelayout.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableLayoutResponse {

    private String id;

    private String layoutName;

    private int gridSizeX;

    private int gridSizeY;

    private int totalTables;

    private int totalCapacity;

    private List<TableDetailsResponse> tables;
}
