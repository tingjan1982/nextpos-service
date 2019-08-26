package io.nextpos.tablelayout.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableDetailsResponse {

    private String tableId;

    private String tableName;

    private int xCoordinate;

    private int yCoordinate;

    private int capacity;
}
