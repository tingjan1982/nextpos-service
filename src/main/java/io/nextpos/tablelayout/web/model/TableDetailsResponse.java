package io.nextpos.tablelayout.web.model;

import io.nextpos.tablelayout.data.TableLayout;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableDetailsResponse {

    private String tableId;

    private String tableName;

    private int capacity;

    private TableLayout.TableDetails.ScreenPosition position;

    public static TableDetailsResponse fromTableDetails(TableLayout.TableDetails tableDetails) {
        return new TableDetailsResponse(tableDetails.getId(),
                tableDetails.getTableName(),
                tableDetails.getCapacity(),
                tableDetails.getScreenPosition());
    }
}
