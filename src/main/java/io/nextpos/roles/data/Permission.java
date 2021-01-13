package io.nextpos.roles.data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Permission {

    /**
     * Order related
     */
    SHIFT,
    ORDER,
    DISCOUNT,
    MEMBERSHIP,

    /**
     * Product related
     */
    PRODUCT,
    PRODUCT_TOGGLES,
    OFFER,

    /**
     * Client and client user related permission.
     */
    CLIENT,
    CLIENT_USER,
    CURRENT_USER,
    USER_ROLE,
    TIME_CARD,

    /**
     * Other setting related
     */
    TABLE_LAYOUT,
    TABLE,
    WORKING_AREA,
    PRINTER,
    ANNOUNCEMENT,
    EINVOICE,

    /**
     * Advanced feature related
     */
    REPORT,
    ROSTER;

    public static List<String> ALL_PERMISSION;

    static {
        ALL_PERMISSION = Arrays.stream(Permission.values()).map(p -> p.toString(Operation.ALL)).collect(Collectors.toList());
    }

    public static List<String> allPermissions() {
        return ALL_PERMISSION;
    }

    public String toString(Operation op) {
        return op.name().toLowerCase() + ":" + this.name().toLowerCase();
    }

    public enum Operation {
        READ, WRITE, DELETE, ALL
    }
}
