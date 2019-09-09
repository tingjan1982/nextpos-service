package io.nextpos.product.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

// todo: move to a shared package.
@Data
@AllArgsConstructor
public class SimpleObjectResponse {

    private String id;

    private String name;
}
