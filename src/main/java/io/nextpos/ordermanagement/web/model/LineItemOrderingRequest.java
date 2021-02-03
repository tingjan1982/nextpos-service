package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.service.bean.LineItemOrdering;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
public class LineItemOrderingRequest {

    @NotNull
    private List<LineItemOrdering> lineItemOrderings;
}
