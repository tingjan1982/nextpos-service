package io.nextpos.shared.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SimpleObjectsResponse {

    private List<SimpleObjectResponse> results;
}
