package io.nextpos.roles.data;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity(name = "client_user_role")
@NamedEntityGraph(name = "UserRole.clientUsers", attributeNodes = @NamedAttributeNode("clientUsers"))
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UserRole extends BaseObject implements ClientObject {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne
    @JoinColumn(name = "clientId")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Client client;

    private String name;

    @OneToMany(mappedBy = "userRole", cascade = CascadeType.MERGE)
    @MapKey
    @Fetch(FetchMode.SUBSELECT)
    private Map<ClientUser.ClientUserId, ClientUser> clientUsers = new HashMap<>();

    /**
     * Stores the permission bundles for role screen rendering.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    //@Enumerated(EnumType.STRING)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "client_user_role_permission_bundle", joinColumns = @JoinColumn(name = "role_id"))
    private Set<PermissionBundle> permissionBundles = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "client_user_role_permission", joinColumns = @JoinColumn(name = "role_id"))
    private List<UserPermission> permissions = new ArrayList<>();

    
    public UserRole(Client client, String name) {
        this.client = client;
        this.name = name;
    }

    public void updatePermissionBundle(Set<PermissionBundle> permissionBundlesToUpdate) {

        permissionBundles.clear();
        permissions.clear();

        final Set<PermissionBundle> updatedPermissionBundles = new HashSet<>(permissionBundlesToUpdate);
        updatedPermissionBundles.add(PermissionBundle.BASE);
        this.setPermissionBundles(updatedPermissionBundles);

        updatedPermissionBundles.stream()
                .flatMap(p -> p.getUserPermissions().stream()).forEach(this::addUserPermission);
    }

    public void addPermissionBundle(PermissionBundle permissionBundle) {
        updatePermissionBundle(Set.of(permissionBundle));
    }

    public void addUserPermission(UserPermission userPermission) {
        permissions.add(userPermission);
    }

    public void addUserPermission(Permission permission, Permission.Operation operation) {
        this.addUserPermission(UserPermission.of(permission, operation));
    }

    public String getPermissionsAsString() {
        return permissions.stream()
                .map(UserPermission::toPermissionString)
                .collect(Collectors.joining(","));
    }

    public void putClientUser(ClientUser clientUser) {
        this.clientUsers.putIfAbsent(clientUser.getId(), clientUser);
    }

    public void removeClientUser(ClientUser clientUser) {
        this.clientUsers.remove(clientUser.getId());
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    public static class UserPermission {

        @Enumerated(EnumType.STRING)
        private Permission permission;

        @Enumerated(EnumType.STRING)
        private Permission.Operation operation;

        public UserPermission(Permission permission, Permission.Operation operation) {
            this.permission = permission;
            this.operation = operation;
        }

        public static UserPermission of(Permission permission, Permission.Operation operation) {
            return new UserPermission(permission, operation);
        }

        public String toPermissionString() {
            return permission.toString(operation);
        }

        public String toWildcardPermission() {
            return ".*:" + permission.name().toLowerCase();
        }
    }
}
