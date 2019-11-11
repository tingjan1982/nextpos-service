package io.nextpos.client.data;

import io.micrometer.core.instrument.util.StringUtils;
import io.nextpos.shared.config.SecurityConfig;
import io.nextpos.shared.model.BaseObject;
import lombok.*;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.io.Serializable;

@Entity(name = "client_user")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ClientUser extends BaseObject {

    @EmbeddedId
    private ClientUserId id;

    private String nickname;

    private String password;

    private String roles;

    public ClientUser(final ClientUserId id, final String password, final String roles) {
        this.id = id;
        this.password = password;
        this.roles = roles;
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
