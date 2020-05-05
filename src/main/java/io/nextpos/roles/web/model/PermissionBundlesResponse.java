package io.nextpos.roles.web.model;

import io.nextpos.roles.data.PermissionBundle;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class PermissionBundlesResponse {

    private Map<PermissionBundle, String> permissions;
}
