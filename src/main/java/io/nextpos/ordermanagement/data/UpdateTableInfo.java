package io.nextpos.ordermanagement.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class UpdateTableInfo {

    private List<Order.TableInfo> fromTables = new ArrayList<>();

    private List<Order.TableInfo> toTables = new ArrayList<>();

    public UpdateTableInfo(Order order) {
        fromTables = (List) order.getMetadata(Order.PREVIOUS_TABLES);
        toTables = order.getTables();
    }

    public boolean hasChange() {
        return CollectionUtils.isNotEmpty(fromTables) && CollectionUtils.isNotEmpty(toTables)
                && !CollectionUtils.isEqualCollection(fromTables, toTables);
    }
}
