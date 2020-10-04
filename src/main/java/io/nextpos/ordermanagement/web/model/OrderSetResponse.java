package io.nextpos.ordermanagement.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OrderSetResponse {

    private String id;

    private String mainOrderId;

    private List<String> orderIds;
}
