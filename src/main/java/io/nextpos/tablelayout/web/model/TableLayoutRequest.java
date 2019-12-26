package io.nextpos.tablelayout.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableLayoutRequest {

    @NotEmpty
    private String layoutName;
}
