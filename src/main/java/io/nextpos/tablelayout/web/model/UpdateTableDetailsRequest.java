package io.nextpos.tablelayout.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.PositiveOrZero;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTableDetailsRequest {

    @NotEmpty
    private String tableName;

    @PositiveOrZero
    private int capacity;
}
