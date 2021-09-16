package io.nextpos.client.data;

import io.nextpos.roles.data.UserRole;
import io.nextpos.shared.config.SecurityConfig;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.workingarea.data.WorkingArea;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity(name = "client_users")
@NamedEntityGraph(name = "ClientUser.userRole",
        attributeNodes = @NamedAttributeNode(value = "userRole", subgraph = "ClientUser.userRole.clientUsers"),
        subgraphs = {
                @NamedSubgraph(name = "ClientUser.userRole.clientUsers",
                        attributeNodes = {
                                @NamedAttributeNode("clientUsers")
                        })
        }
)
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class ClientUser extends BaseObject {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id")
    private String id;

    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Client client;

    /**
     * This exists solely for the convenience of frontend passing username value as id for authentication.
     */
    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
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

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns = @JoinColumn(name = "client_user_id"), inverseJoinColumns = @JoinColumn(name = "working_area_id"))
    @Fetch(FetchMode.SUBSELECT)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<WorkingArea> workingAreas = new HashSet<>();

    @Deprecated
    public ClientUser(Client client, String username, String password, String roles) {
        this.username = username;
        this.nickname = username;
        this.client = client;
        this.password = password;
        this.roles = roles;
    }

    public ClientUser(Client client, String username, String nickname, String password, String roles) {
        this.client = client;
        this.username = username;
        this.nickname = nickname;
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

    public void addWorkingArea(WorkingArea workingArea) {
        workingAreas.add(workingArea);
        workingArea.getClientUsers().add(this);
    }

    public void clearWorkingAreas() {
        workingAreas.forEach(p -> p.getClientUsers().removeIf(next -> StringUtils.equals(next.getId(), this.getId())));
        workingAreas.clear();
    }

    /**
     * Represent a meaningful name. todo: change to return nickname when ClientUserRequest.nickname has NotBlank validation.
     */
    public String getName() {
        return StringUtils.isNotBlank(nickname) ? nickname : username;
    }

    public boolean isDefaultUser() {
        return roles.contains(SecurityConfig.Role.ADMIN_ROLE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientUser that = (ClientUser) o;
        return Objects.equals(id, that.id) && client.equals(that.client) && username.equals(that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, client, username);
    }
}
