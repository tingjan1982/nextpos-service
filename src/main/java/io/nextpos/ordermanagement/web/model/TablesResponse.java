package io.nextpos.ordermanagement.web.model;

import io.nextpos.shared.web.model.SimpleObjectResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class TablesResponse {

    Map<String, List<SimpleObjectResponse>> availableTables;
}
