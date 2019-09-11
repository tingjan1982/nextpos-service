package io.nextpos.client.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientUserResponse {

    private String nickname;

    private String username;

    private String encryptedPassword;

    private List<String> roles;

    private boolean defaultUser;
}
