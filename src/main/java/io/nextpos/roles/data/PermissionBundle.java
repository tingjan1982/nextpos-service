package io.nextpos.roles.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * PermissionBundle maps more closely to the wordings used in the UI, whereas Permission maps closely with backend business object.
 */
public enum PermissionBundle {

    BASE("base", Arrays.asList(
            UserRole.UserPermission.of(Permission.CLIENT, Permission.Operation.READ),
            UserRole.UserPermission.of(Permission.CLIENT_USER, Permission.Operation.READ),
            UserRole.UserPermission.of(Permission.TIME_CARD, Permission.Operation.READ),
            UserRole.UserPermission.of(Permission.TIME_CARD, Permission.Operation.WRITE),
            UserRole.UserPermission.of(Permission.PRODUCT, Permission.Operation.READ),
            UserRole.UserPermission.of(Permission.ORDER, Permission.Operation.READ),
            UserRole.UserPermission.of(Permission.SHIFT, Permission.Operation.READ),
            UserRole.UserPermission.of(Permission.TABLE_LAYOUT, Permission.Operation.READ),
            UserRole.UserPermission.of(Permission.TABLE, Permission.Operation.READ),
            UserRole.UserPermission.of(Permission.WORKING_AREA, Permission.Operation.READ),
            UserRole.UserPermission.of(Permission.PRINTER, Permission.Operation.READ),
            UserRole.UserPermission.of(Permission.ANNOUNCEMENT, Permission.Operation.READ)
    )),

    CREATE_ORDER("createOrder", Collections.singletonList(
            UserRole.UserPermission.of(Permission.ORDER, Permission.Operation.WRITE))),

    DELETE_ORDER("deleteOrder", Collections.singletonList(
            UserRole.UserPermission.of(Permission.ORDER, Permission.Operation.DELETE)
    )),

    CLOSE_SHIFT("closeShift", Collections.singletonList(
            UserRole.UserPermission.of(Permission.SHIFT, Permission.Operation.WRITE)
    )),

    APPLY_DISCOUNT("applyDiscount", Collections.singletonList(
            UserRole.UserPermission.of(Permission.DISCOUNT, Permission.Operation.WRITE)
    )),

    MANAGE_PRODUCT("manageProduct", Arrays.asList(
            UserRole.UserPermission.of(Permission.PRODUCT, Permission.Operation.WRITE),
            UserRole.UserPermission.of(Permission.PRODUCT, Permission.Operation.DELETE)
    )),

    MANAGE_STAFF("manageStaff", Arrays.asList(
            UserRole.UserPermission.of(Permission.CLIENT_USER, Permission.Operation.WRITE),
            UserRole.UserPermission.of(Permission.CLIENT_USER, Permission.Operation.DELETE)
    )),

    MANAGE_STORE("manageStore", Collections.singletonList(
            UserRole.UserPermission.of(Permission.CLIENT, Permission.Operation.WRITE)
    )),
    
    MANAGE_SETTINGS("manageSettings", Arrays.asList(

            UserRole.UserPermission.of(Permission.TABLE_LAYOUT, Permission.Operation.WRITE),
            UserRole.UserPermission.of(Permission.TABLE_LAYOUT, Permission.Operation.DELETE),
            UserRole.UserPermission.of(Permission.TABLE, Permission.Operation.WRITE),
            UserRole.UserPermission.of(Permission.TABLE, Permission.Operation.DELETE),
            UserRole.UserPermission.of(Permission.WORKING_AREA, Permission.Operation.WRITE),
            UserRole.UserPermission.of(Permission.WORKING_AREA, Permission.Operation.DELETE),
            UserRole.UserPermission.of(Permission.PRINTER, Permission.Operation.WRITE),
            UserRole.UserPermission.of(Permission.PRINTER, Permission.Operation.DELETE)
    )),

    MANAGE_ANNOUNCEMENT("manageAnnouncement", Collections.singletonList(
            UserRole.UserPermission.of(Permission.ANNOUNCEMENT, Permission.Operation.WRITE)
    )),

    VIEW_REPORT("viewReport", Collections.singletonList(
            UserRole.UserPermission.of(Permission.REPORT, Permission.Operation.READ)
    ));

    private String messageKey;

    private List<UserRole.UserPermission> userPermissions;


    PermissionBundle(final String messageKey, final List<UserRole.UserPermission> userPermissions) {
        this.messageKey = messageKey;
        this.userPermissions = userPermissions;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public List<UserRole.UserPermission> getUserPermissions() {
        return userPermissions;
    }
}
