package io.nextpos.roles.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public enum Permission {

    SHIFT,
    ORDER,
    DISCOUNT,

    PRODUCT,

    CLIENT,
    CLIENT_USER,
    TIME_CARD,

    TABLE_LAYOUT,
    TABLE,
    WORKING_AREA,
    PRINTER,

    REPORT,
    ANNOUNCEMENT;

    public static List<String> ALL_PERMISSION;

    static {
        ALL_PERMISSION = Arrays.stream(Permission.values()).map(p -> {
            return Arrays.stream(Operation.values()).map(p::toString).collect(Collectors.toList());
        }).flatMap(Collection::stream).collect(Collectors.toList());
    }

    public static List<String> allPermissions() {
        return ALL_PERMISSION;
    }

    public String toString(Operation op) {
        return op.name().toLowerCase() + ":" + this.name().toLowerCase();
    }

    public enum Operation {
        READ, WRITE, DELETE
    }
}
