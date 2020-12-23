package io.nextpos.ordermanagement.web;

import io.nextpos.ordermanagement.data.SplitAmountDetails;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SplitByHeadCountResponse {

    private int headCount;

    private List<SplitAmountDetails> splitAmounts;

    public SplitByHeadCountResponse(List<SplitAmountDetails> splitAmounts) {
        this.splitAmounts = splitAmounts;
        this.headCount = splitAmounts.size();
    }
}
