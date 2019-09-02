package io.nextpos.ordermanagement.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Shift extends MongoBaseObject {

    @Id
    private String id;

    private String clientId;
                                   
    private ShiftDetails start;

    private List<ShiftDetails> interimBalances = new ArrayList<>();

    private ShiftDetails end;

    private String unbalanceReason;

    private ShiftStatus shiftStatus;

    public Shift(final String clientId, final Date startTimestamp, final String shiftStartBy, final BigDecimal openingBalance) {
        this.clientId = clientId;

        start = new ShiftDetails(startTimestamp, shiftStartBy, openingBalance);
        end = new ShiftDetails();
        shiftStatus = ShiftStatus.ACTIVE;
    }

    public void addInterimBalance(ShiftDetails shiftDetails) {
        interimBalances.add(shiftDetails);
    }

    public void closeShift(String closedBy, BigDecimal closingBalance) {
        end.setTimestamp(new Date());
        end.setWho(closedBy);
        end.setBalance(closingBalance);

        if (closingBalance.equals(start.balance)) {
            shiftStatus = ShiftStatus.BALANCED;
        } else {
            shiftStatus = ShiftStatus.UNBALANCED;
        }
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    @Data
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    public static class ShiftDetails {

        private Date timestamp;

        private String who;

        private BigDecimal balance;

        public ShiftDetails(final Date timestamp, final String who, final BigDecimal balance) {
            this.timestamp = timestamp;
            this.who = who;
            this.balance = balance;
        }
    }

    public enum ShiftStatus {

        /**
         * Indicates no active shift.
         */
        INACTIVE,

        /**
         * Indicates shift is active.
         */
        ACTIVE,

        /**
         * Shift is ended and closing balance matches the opening balance.
         */
        BALANCED,

        /**
         * Opposite of BALANCED.
         */
        UNBALANCED
    }
}
