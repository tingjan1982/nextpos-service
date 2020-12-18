package io.nextpos.ordermanagement.data;

import io.nextpos.membership.data.Membership;
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

    public static class UpdateMembership implements Consumer<Order> {

        private final Membership membership;

        private UpdateMembership(Membership membership) {
            this.membership = membership;
        }

        public static UpdateMembership instance(Membership membership) {
            return new UpdateMembership(membership);
        }

        @Override
        public void accept(Order order) {
            order.setMembership(membership);
            order.setLookupMembershipId(membership.getId());
        }
    }
}
