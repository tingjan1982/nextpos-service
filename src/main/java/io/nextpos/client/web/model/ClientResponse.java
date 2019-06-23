package io.nextpos.client.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponse {

    private String id;

    private String clientName;

    private String username;

    private String encryptedPassword;
}
