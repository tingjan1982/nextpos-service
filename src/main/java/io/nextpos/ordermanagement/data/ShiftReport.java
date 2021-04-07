package io.nextpos.ordermanagement.data;

import io.nextpos.ordertransaction.data.ClosingShiftTransactionReport;
import io.nextpos.ordertransaction.data.OrderTransaction;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class ShiftReport {

    private Date startDate;

    private Date endDate;

    private ClosingShiftTransactionReport.OrderSummary orderSummary;

    private PaymentMethodSummary cash = new PaymentMethodSummary();

    private PaymentMethodSummary card = new PaymentMethodSummary();

    private int totalOrderCount;

    private Map<Order.OrderState, ClosingShiftTransactionReport.OrderCount> orderCounts;

    private List<Shift.DeletedLineItem> deletedLineItems;

    private BigDecimal deletedLineItemsTotal;

    private String closingRemark;

    public ShiftReport(Shift shift) {

        startDate = shift.getStart().getTimestamp();
        final Shift.CloseShiftDetails closeShift = shift.getEnd();
        endDate = closeShift.getTimestamp();

        updatePaymentMethodSummary(cash, closeShift, OrderTransaction.PaymentMethod.CASH);
        updatePaymentMethodSummary(card, closeShift, OrderTransaction.PaymentMethod.CARD);

        orderSummary = closeShift.getClosingShiftReport().getOneOrderSummary();

        totalOrderCount = closeShift.getClosingShiftReport().getTotalOrderCount();

        orderCounts = closeShift.getClosingShiftReport().getOrderCountByState();

        deletedLineItems = shift.getDeletedLineItems();

        deletedLineItemsTotal = deletedLineItems.stream()
                .map(Shift.DeletedLineItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        closingRemark = closeShift.getClosingRemark();
    }

    private void updatePaymentMethodSummary(PaymentMethodSummary paymentMethodSummary,
                                            Shift.CloseShiftDetails closeShift,
                                            OrderTransaction.PaymentMethod paymentMethod) {

        final ClosingShiftTransactionReport closingShiftReport = closeShift.getClosingShiftReport();

        closingShiftReport.getShiftTotal(paymentMethod).ifPresent(t -> paymentMethodSummary.setTotal(t.getOrderTotal()));

        final Shift.ClosingBalanceDetails closingBalance = closeShift.getClosingBalance(paymentMethod);
        paymentMethodSummary.setClosingBalance(closingBalance.getClosingBalance());
        paymentMethodSummary.setDifference(closingBalance.getDifference());
        paymentMethodSummary.setUnbalanceReason(closingBalance.getUnbalanceReason());
    }

    @Data
    public static class PaymentMethodSummary {
        
        private BigDecimal total = BigDecimal.ZERO;

        private BigDecimal closingBalance = BigDecimal.ZERO;

        private BigDecimal difference = BigDecimal.ZERO;

        private String unbalanceReason;
    }
}
