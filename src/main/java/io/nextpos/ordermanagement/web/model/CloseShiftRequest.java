package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Shift;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class CloseShiftRequest {

    @Valid
    private Shift.ClosingBalanceDetails cash;

    @Valid
    private Shift.ClosingBalanceDetails card;

    private Map<String, Shift.ClosingBalanceDetails> closingBalances = new HashMap<>();
}
