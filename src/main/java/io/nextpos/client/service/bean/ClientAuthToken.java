package io.nextpos.client.service.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientAuthToken {

    private String username;

    private String password;
}
