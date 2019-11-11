package io.nextpos.shared.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeleteObjectResponse {

    private String deletedId;
}
