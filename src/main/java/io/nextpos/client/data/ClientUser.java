package io.nextpos.client.data;

import io.micrometer.core.instrument.util.StringUtils;
import io.nextpos.roles.data.UserRole;
import io.nextpos.shared.config.SecurityConfig;
import io.nextpos.shared.model.BaseObject;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Entity(name = "client_user")
@NamedEntityGraph(name = "ClientUser.userRole",
        attributeNodes = @NamedAttributeNode(value = "userRole", subgraph = "ClientUser.userRole.clientUsers"),
        subgraphs = {
                @NamedSubgraph(name = "ClientUser.userRole.clientUsers",
                        attributeNodes = {
                                @NamedAttributeNode("clientUsers")
                        })
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ClientUser extends BaseObject {

    @EmbeddedId
    private ClientUserId id;

    @ManyToOne
    @JoinColumn(name = "client_ref_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Client client;

    private String nickname;

    private String password;

    @Deprecated
    private String roles;

    /**
     * Stores user's UserRole and associated permissions.
     */
    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserRole userRole;

    /**
     * Comma separate list of scopes as permissions, which is used in SecurityConfig to determine access privilege.
     */
    @Column(length = 1000)
    private String permissions;

    public ClientUser(ClientUserId id, Client client, String password, String roles) {
        this.id = id;
        this.client = client;
        this.password = password;
        this.roles = roles;
    }

    public void setUserRole(final UserRole userRole) {
        this.userRole = userRole;
        this.permissions = userRole.getPermissionsAsString();

        userRole.putClientUser(this);
    }

    public void removeUserRole() {
        if (this.userRole != null) {
            this.userRole.removeClientUser(this);
            this.permissions = "";
            this.userRole = null;
        }
    }


    /**
     * Represent a meaningful name. Nickname comes first.
     */
    public String getName() {
        return StringUtils.isNotBlank(nickname) ? nickname : id.username;
    }

    public boolean isDefaultUser() {
        return roles.contains(SecurityConfig.Role.ADMIN_ROLE);
    }

    /**
     * https://www.baeldung.com/jpa-composite-primary-keys
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientUserId implements Serializable {

        private String username;

        // todo: this should be renamed to clientUsername to avoid confusion.
        private String clientId;
    }
}
