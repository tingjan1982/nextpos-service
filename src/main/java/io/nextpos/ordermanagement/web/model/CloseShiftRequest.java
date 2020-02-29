package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Shift;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;

@Data
@NoArgsConstructor
public class CloseShiftRequest {

    @Valid
    private Shift.ClosingBalanceDetails cash;

    @Valid
    private Shift.ClosingBalanceDetails card;
}
