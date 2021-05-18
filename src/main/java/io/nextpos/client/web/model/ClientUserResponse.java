package io.nextpos.client.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ClientUserResponse {

    private String id;

    /**
     * Used for display purpose.
     */
    private String displayName;

    /**
     * This is used for edit user screen.
     */
    private String nickname;

    /**
     * This is used for authentication.
     */
    private String username;

    private String encryptedPassword;

    private List<String> roles;

    private String userRoleId;

    private List<String> workingAreaIds;

    private boolean defaultUser;

    private String permissions;
}
