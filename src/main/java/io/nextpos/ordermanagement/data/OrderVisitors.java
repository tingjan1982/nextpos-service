package io.nextpos.ordermanagement.data;

import io.nextpos.tablelayout.data.TableLayout;

import java.util.List;
import java.util.function.Consumer;

/**
 * Extract related order related logics into separate Visitor implementation.
 */
public class OrderVisitors {

    public static void accept(Order order, Consumer<Order> orderOperation) {
        orderOperation.accept(order);
    }

    /**
     * Used to update order tables.
     */
    public static class UpdateTables implements Consumer<Order> {

        private final List<TableLayout.TableDetails> tables;

        private UpdateTables(List<TableLayout.TableDetails> tables) {
            this.tables = tables;
        }

        public static UpdateTables instance(List<TableLayout.TableDetails> tables) {
            return new UpdateTables(tables);
        }

        @Override
        public void accept(Order order) {

            if (!tables.isEmpty()) {
                order.getTables().clear();

                tables.forEach(t -> {
                    final Order.TableInfo tableInfo = new Order.TableInfo(t);
                    order.getTables().add(tableInfo);
                });
            }
        }
    }
}
