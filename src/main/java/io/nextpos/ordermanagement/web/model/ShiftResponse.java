package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordertransaction.data.ClosingShiftTransactionReport;
import io.nextpos.ordertransaction.data.OrderTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftResponse {

    private String id;

    private String clientId;

    private Shift.ShiftStatus shiftStatus;

    private OpenShiftDetailsResponse open;

    private CloseShiftDetailsResponse close;

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
