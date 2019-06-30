package io.nextpos.client.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientUserRequest {

    private String username;

    private String password;

    private List<String> roles;
}
