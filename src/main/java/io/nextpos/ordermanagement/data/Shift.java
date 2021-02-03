package io.nextpos.ordermanagement.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.nextpos.ordertransaction.data.ClosingShiftTransactionReport;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.util.DateTimeUtil;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Shift extends MongoBaseObject {

    @Id
    private String id;

    private String clientId;

    private OpenShiftDetails start;

    private CloseShiftDetails end;

    private ShiftStatus shiftStatus;

    private List<DeletedLineItem> deletedLineItems = new ArrayList<>();

    public Shift(final String clientId, final Date startTimestamp, final String shiftStartBy, final BigDecimal openingBalance) {
        this.clientId = clientId;

        start = new OpenShiftDetails(startTimestamp, shiftStartBy, openingBalance);
        end = new CloseShiftDetails();
        shiftStatus = ShiftStatus.ACTIVE;
    }

    public void initiateCloseShift(Function<Shift, ClosingShiftTransactionReport> closingShiftTransactionReport) {

        end.setTimestamp(new Date());
        end.setClosingShiftReport(closingShiftTransactionReport.apply(this));
        shiftStatus = ShiftStatus.CLOSING;
    }

    public void balanceClosingShift(Function<Shift, ClosingShiftTransactionReport> closingShiftTransactionReport) {

        end.setClosingShiftReport(closingShiftTransactionReport.apply(this));
        end.balanceClosingShift(OrderTransaction.PaymentMethod.CASH, start.balance);
        end.balanceClosingShift(OrderTransaction.PaymentMethod.CARD, BigDecimal.ZERO);
    }

    public void closeShift(String closedBy, ClosingBalanceDetails cash, ClosingBalanceDetails card) {

        end.setWho(closedBy);
        end.updateClosingBalanceDetails(cash, OrderTransaction.PaymentMethod.CASH, start.balance);
        end.updateClosingBalanceDetails(card, OrderTransaction.PaymentMethod.CARD, BigDecimal.ZERO);

        shiftStatus = ShiftStatus.CONFIRM_CLOSE;
    }

    public void confirmCloseShift(String closingRemark) {
        end.setClosingRemark(closingRemark);

        if (end.checkClosingBalance()) {
            shiftStatus = ShiftStatus.BALANCED;
        } else {
            shiftStatus = ShiftStatus.UNBALANCED;
        }
    }

    public void abortCloseShift() {
        end = new CloseShiftDetails();
        shiftStatus = ShiftStatus.ACTIVE;
    }

    public void addDeletedLineItem(Order order, OrderLineItem orderLineItem, String username) {

        final DeletedLineItem deletedLineItem = new DeletedLineItem(order.getId(),
                orderLineItem.getId(),
                orderLineItem.getProductSnapshot().getName(),
                orderLineItem.getQuantity(),
                orderLineItem.getLineItemSubTotal(),
                new Date(),
                username);

        deletedLineItems.add(deletedLineItem);
    }

    public void removeDeletedLineItem(OrderLineItem orderLineItem) {
        deletedLineItems.removeIf(li -> li.getLineItemId().equals(orderLineItem.getId()));
    }

    @Data
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    @AllArgsConstructor
    public static class OpenShiftDetails {

        private Date timestamp;

        private String who;

        /**
         * Opening balance.
         */
        private BigDecimal balance;

        public LocalDateTime toLocalDateTime(ZoneId zoneId) {
            return DateTimeUtil.toLocalDateTime(zoneId, timestamp);
        }
    }

    @Data
    @NoArgsConstructor
    public static class CloseShiftDetails {

        private Date timestamp;

        private String who;

        private ClosingShiftTransactionReport closingShiftReport;

        private Map<OrderTransaction.PaymentMethod, ClosingBalanceDetails> closingBalances = new HashMap<>();

        private String closingRemark;

        public ClosingBalanceDetails getClosingBalance(OrderTransaction.PaymentMethod paymentMethod) {
            return closingBalances.getOrDefault(paymentMethod, new ClosingBalanceDetails());
        }

        public void balanceClosingShift(OrderTransaction.PaymentMethod paymentMethod, final BigDecimal startingBalance) {

            closingShiftReport.getShiftTotal(paymentMethod).ifPresent(total -> {
                final BigDecimal orderTotal = total.getOrderTotal();

                if (closingBalances.containsKey(paymentMethod)) {
                    final ClosingBalanceDetails closingBalanceDetails = closingBalances.get(paymentMethod);
                    closingBalanceDetails.setClosingBalance(orderTotal.add(startingBalance));
                    closingBalanceDetails.setDifference(BigDecimal.ZERO);
                }
            });
        }

        public void updateClosingBalanceDetails(ClosingBalanceDetails closingBalanceDetails, OrderTransaction.PaymentMethod paymentMethod, final BigDecimal startingBalance) {

            if (closingBalanceDetails != null) {
                closingBalances.put(paymentMethod, closingBalanceDetails);

                closingShiftReport.getShiftTotal(paymentMethod).ifPresent(p -> {
                    final BigDecimal orderTotal = p.getOrderTotal();
                    BigDecimal difference = closingBalanceDetails.getClosingBalance().subtract(orderTotal).subtract(startingBalance);
                    closingBalanceDetails.setDifference(difference);
                });
            }
        }

        public boolean checkClosingBalance() {
            return closingBalances.values().stream().allMatch(ClosingBalanceDetails::isBalanced);
        }

        public LocalDateTime toLocalDateTime(ZoneId zoneId) {

            if (timestamp != null) {
                return DateTimeUtil.toLocalDateTime(zoneId, timestamp);
            }

            return LocalDateTime.now().atZone(ZoneId.systemDefault()).withZoneSameInstant(zoneId).toLocalDateTime();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClosingBalanceDetails {

        @PositiveOrZero
        private BigDecimal closingBalance = BigDecimal.ZERO;

        private BigDecimal difference = BigDecimal.ZERO;

        private String unbalanceReason;

        public static ClosingBalanceDetails of(BigDecimal closingBalance) {
            return new ClosingBalanceDetails(closingBalance, BigDecimal.ZERO, null);
        }

        @JsonIgnore
        public boolean isBalanced() {
            return BigDecimal.ZERO.compareTo(difference) == 0;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeletedLineItem {

        private String orderId;

        private String lineItemId;

        private String productName;

        private int quantity;

        private BigDecimal total;

        private Date deletedDate;

        private String deletedBy;
    }

    public enum ShiftStatus {

        /**
         * Indicates no active shift.
         */
        INACTIVE(false),

        /**
         * Indicates shift is active.
         */
        ACTIVE(false),

        /**
         * Indicates shift has initiated closing.
         */
        CLOSING(false),

        /**
         * Indicates shift is pending confirmation to close.
         */
        CONFIRM_CLOSE(false),

        /**
         * Shift is ended and closing balance matches the opening balance.
         */
        BALANCED(true),

        /**
         * Opposite of BALANCED.
         */
        UNBALANCED(true);

        private final boolean finalState;

        ShiftStatus(boolean finalState) {
            this.finalState = finalState;
        }

        public boolean isFinalState() {
            return finalState;
        }
    }

    public enum ShiftAction {

        INITIATE_CLOSE(EnumSet.of(ShiftStatus.ACTIVE, ShiftStatus.CLOSING, ShiftStatus.CONFIRM_CLOSE)),

        CLOSE(EnumSet.of(ShiftStatus.CLOSING, ShiftStatus.CONFIRM_CLOSE)),

        CONFIRM_CLOSE(EnumSet.of(ShiftStatus.CONFIRM_CLOSE)),

        ABORT_CLOSE(EnumSet.of(ShiftStatus.CLOSING, ShiftStatus.CONFIRM_CLOSE));

        private final EnumSet<ShiftStatus> validFromState;

        ShiftAction(final EnumSet<ShiftStatus> validFromState) {
            this.validFromState = validFromState;
        }

        public void checkShiftStatus(Shift shift) {

            final ShiftStatus shiftStatus = shift.getShiftStatus();

            if (!validFromState.contains(shiftStatus)) {
                final String message = String.format("Current shift status %s cannot execute this shift action[%s]. Valid shift status is: %s", shiftStatus, this, validFromState);
                throw new BusinessLogicException(message);
            }
        }
    }
}
