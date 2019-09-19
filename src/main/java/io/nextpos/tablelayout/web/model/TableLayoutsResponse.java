package io.nextpos.tablelayout.web.model;

import io.nextpos.shared.web.model.SimpleObjectResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TableLayoutsResponse {

    private List<SimpleObjectResponse> tableLayouts;
}
