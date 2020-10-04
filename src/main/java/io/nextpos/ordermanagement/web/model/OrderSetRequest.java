package io.nextpos.ordermanagement.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;
import java.util.List;

@Data
@NoArgsConstructor
public class OrderSetRequest {

    @Size(min = 2)
    private List<String> orderIds;
}
