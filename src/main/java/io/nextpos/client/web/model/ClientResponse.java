package io.nextpos.client.web.model;

import io.nextpos.client.data.Client;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponse {

    private String id;

    private String clientName;

    private String username;

    private String encryptedPassword;

    private String country;

    private Client.Status status;

    private Map<String, String> attributes;
}
