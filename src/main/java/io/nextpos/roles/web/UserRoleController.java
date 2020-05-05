package io.nextpos.roles.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.roles.data.PermissionBundle;
import io.nextpos.roles.data.UserRole;
import io.nextpos.roles.service.UserRoleService;
import io.nextpos.roles.web.model.PermissionBundlesResponse;
import io.nextpos.roles.web.model.UserRoleRequest;
import io.nextpos.roles.web.model.UserRoleResponse;
import io.nextpos.roles.web.model.UserRolesResponse;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/roles")
public class UserRoleController {

    private final UserRoleService userRoleService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    @Autowired
    public UserRoleController(final UserRoleService userRoleService, final ClientObjectOwnershipService clientObjectOwnershipService) {
        this.userRoleService = userRoleService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
    }

    @PostMapping
    public UserRoleResponse createUserRole(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                           @Valid @RequestBody UserRoleRequest userRoleRequest) {

        UserRole userRole = fromUserRoleRequest(client, userRoleRequest);

        return toUserRoleResponse(userRoleService.saveUserRole(userRole));
    }

    private UserRole fromUserRoleRequest(final Client client, final UserRoleRequest userRoleRequest) {

        final UserRole userRole = new UserRole(client, userRoleRequest.getRoleName());
        userRole.updatePermissionBundle(userRoleRequest.getPermissions());

        return userRole;
    }

    @GetMapping
    public UserRolesResponse getUserRoles(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final List<UserRole> userRoles = userRoleService.getUserRoles(client);
        final List<UserRoleResponse> userRoleResponses = userRoles.stream()
                .map(this::toUserRoleResponse).collect(Collectors.toList());

        return new UserRolesResponse(userRoleResponses);
    }

    @GetMapping("/{id}")
    public UserRoleResponse createUserRole(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                           @PathVariable String id) {

        final UserRole userRole = clientObjectOwnershipService.checkOwnership(client, () -> userRoleService.getUserRole(id));
        return toUserRoleResponse(userRole);
    }

    @PostMapping("/{id}")
    public UserRoleResponse createUserRole(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                           @PathVariable String id,
                                           @Valid @RequestBody UserRoleRequest userRoleRequest) {

        final UserRole userRole = clientObjectOwnershipService.checkOwnership(client, () -> userRoleService.getUserRole(id));

        updateUserRoleFromRequest(userRole, userRoleRequest);

        return toUserRoleResponse(userRoleService.updateUserRole(userRole));
    }

    private void updateUserRoleFromRequest(final UserRole userRole, final UserRoleRequest userRoleRequest) {

        userRole.setName(userRoleRequest.getRoleName());
        userRole.updatePermissionBundle(userRoleRequest.getPermissions());
    }

    private UserRoleResponse toUserRoleResponse(final UserRole userRole) {
        return new UserRoleResponse(userRole.getId(), userRole.getName(), userRole.getPermissionBundles());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserRole(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                           @PathVariable String id) {

        final UserRole userRole = clientObjectOwnershipService.checkOwnership(client, () -> userRoleService.getUserRole(id));

        userRoleService.deleteUserRole(userRole);

    }

    @GetMapping("/permissions")
    public PermissionBundlesResponse permissions() {

        final Map<PermissionBundle, String> permissionBundles = Arrays.stream(PermissionBundle.values())
                .filter(p -> p != PermissionBundle.BASE)
                .collect(Collectors.toMap(p -> p, PermissionBundle::getMessageKey));
        return new PermissionBundlesResponse(permissionBundles);
    }
}
