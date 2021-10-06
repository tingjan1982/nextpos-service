package io.nextpos.ordermanagement.data;

import io.nextpos.membership.data.Membership;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.workingarea.service.WorkingAreaService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Extract related order related logics into separate Visitor implementation.
 */
public class OrderVisitors {

    public static void accept(Order order, Consumer<Order> orderOperation) {
        orderOperation.accept(order);
    }

    public static <T> T get(Order order, Function<Order, T> orderOperation) {
        return orderOperation.apply(order);
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
                order.addMetadata(Order.PREVIOUS_TABLES, new ArrayList<>(order.getTables()));
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

            if (membership != null) {
                order.setLookupMembershipId(membership.getId());
            } else {
                order.setLookupMembershipId(null);
            }
        }
    }

    public static class OrderLineItemGrouper implements Function<Order, Map<String, List<OrderLineItem>>> {

        public static final String NO_WORKING_AREA = "noWorkingArea";

        private final WorkingAreaService workingAreaService;

        private OrderLineItemGrouper(WorkingAreaService workingAreaService) {
            this.workingAreaService = workingAreaService;
        }

        public static OrderLineItemGrouper instance(WorkingAreaService workingAreaService) {
            return new OrderLineItemGrouper(workingAreaService);
        }

        @Override
        public Map<String, List<OrderLineItem>> apply(Order order) {

            return order.getOrderLineItems().stream()
                    .filter(oli -> oli.getState().isPreparing())
                    .collect(Collectors.groupingBy(oli -> {
                        if (StringUtils.isNotBlank(oli.getWorkingAreaId())) {
                            return workingAreaService.getWorkingArea(oli.getWorkingAreaId()).getName();
                        } else {
                            return NO_WORKING_AREA;
                        }
                    }));
        }
    }
}
