package io.nextpos.client.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ClientUserResponse {

    private String nickname;

    private String username;

    private String displayName;

    private String encryptedPassword;

    private List<String> roles;

    private String userRoleId;

    private boolean defaultUser;

    private String permissions;
}
