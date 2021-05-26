package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordertransaction.data.ClosingShiftTransactionReport;
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

    private Shift.ShiftStatus shiftStatus;

    private OpenShiftDetailsResponse open;

    private CloseShiftDetailsResponse close;

    private List<Shift.DeletedLineItem> deletedLineItems;

    public ShiftResponse(Shift shift) {

        this.id = shift.getId();
        this.shiftStatus = shift.getShiftStatus();

        this.open = new ShiftResponse.OpenShiftDetailsResponse(
                shift.getStart().getTimestamp(),
                shift.getStart().getWho(),
                shift.getStart().getBalance());

        this.close = new ShiftResponse.CloseShiftDetailsResponse(
                shift.getEnd().getTimestamp(),
                shift.getEnd().getWho(),
                shift.getEnd().getClosingShiftReport(),
                shift.getEnd().getClosingBalances(),
                shift.getEnd().getClosingRemark());

        this.deletedLineItems = shift.getDeletedLineItems();
    }

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

        private Map<String, Shift.ClosingBalanceDetails> closingBalances;

        private String closingRemark;
    }
}
