package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordertransaction.data.ClosingShiftTransactionReport;
import io.nextpos.ordertransaction.data.OrderTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ShiftResponse {

    private String id;

    // todo: is this used?
    private String clientId;

    private Shift.ShiftStatus shiftStatus;

    private OpenShiftDetailsResponse open;

    private CloseShiftDetailsResponse close;

    private List<Shift.DeletedLineItem> deletedLineItems;

    @Data
    @AllArgsConstructor
    public static class OpenShiftDetailsResponse {

        private Date timestamp;

        private String who;

        private BigDecimal balance;
    }

    @Data
    @AllArgsConstructor
    public static class CloseShiftDetailsResponse {

        private Date timestamp;

        private String who;

        private ClosingShiftTransactionReport closingShiftReport;

        private Map<OrderTransaction.PaymentMethod, Shift.ClosingBalanceDetails> closingBalances;

        private String closingRemark;
    }
}
