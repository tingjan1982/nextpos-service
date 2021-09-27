package io.nextpos.ordermanagement.data;

import io.nextpos.client.data.Client;
import io.nextpos.ordertransaction.data.ClosingShiftTransactionReport;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.shared.util.PaymentMethodLocalization;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class ShiftReport {

    private String clientName;

    private Date startDate;

    private Date endDate;

    /**
     * For SendGrid
     */
    private String startDateStr;

    /**
     * For SendGrid
     */
    private String endDateStr;

    private ClosingShiftTransactionReport.OrderSummary orderSummary;

    private BigDecimal startBalance;

    private List<PaymentMethodSummary> summaries;

    private int totalOrderCount;

    private Map<Order.OrderState, ClosingShiftTransactionReport.OrderCount> orderCounts;

    private List<Shift.DeletedLineItem> deletedLineItems;

    private BigDecimal deletedLineItemsTotal;

    private String closingRemark;

    public ShiftReport(Client client, Shift shift) {

        clientName = client.getClientName();

        startDate = shift.getStart().getTimestamp();
        final Shift.CloseShiftDetails closeShift = shift.getEnd();
        endDate = closeShift.getTimestamp();

        startDateStr = DateTimeUtil.formatDateTime(shift.getStart().toLocalDateTime(client.getZoneId()));
        endDateStr = DateTimeUtil.formatDateTime(shift.getEnd().toLocalDateTime(client.getZoneId()));

        startBalance = shift.getStart().getBalance();

        final Map<String, Shift.ClosingBalanceDetails> closingBalances = shift.getEnd().getClosingBalances();

        summaries = closingBalances.entrySet().stream()
                .map(e -> new PaymentMethodSummary(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(PaymentMethodSummary::getPaymentMethod))
                .collect(Collectors.toList());

        orderSummary = closeShift.getClosingShiftReport().getOneOrderSummary();

        totalOrderCount = closeShift.getClosingShiftReport().getTotalOrderCount();

        orderCounts = closeShift.getClosingShiftReport().getOrderCountByState();

        deletedLineItems = shift.getDeletedLineItems();

        deletedLineItemsTotal = deletedLineItems.stream()
                .map(Shift.DeletedLineItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        closingRemark = closeShift.getClosingRemark();
    }

    @Data
    public static class PaymentMethodSummary {

        private String paymentMethod;

        private String paymentMethodDisplayName;

        private BigDecimal closingBalance;

        private BigDecimal total;

        private BigDecimal difference;

        private String unbalanceReason;

        public PaymentMethodSummary(String paymentMethod, Shift.ClosingBalanceDetails closingBalance) {

            this.paymentMethod = paymentMethod;
            this.paymentMethodDisplayName = PaymentMethodLocalization.getPaymentMethodTranslation(paymentMethod);
            this.closingBalance = closingBalance.getClosingBalance();
            this.total = closingBalance.getExpectedBalance();
            this.difference = closingBalance.getDifference();
            this.unbalanceReason = closingBalance.getUnbalanceReason();
        }
    }
}
