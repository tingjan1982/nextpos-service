package io.nextpos.ordermanagement.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class SplitOrderByHeadCountRequest {

    @Min(2)
    @NotNull
    private Integer headCount;
}
