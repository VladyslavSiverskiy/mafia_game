package com.vsiverskyi.model.enums;

public enum ERoleOrder {
    DON,
    MAFIA,
    SHERYF,
    DOCTOR,
    LEDY,
    MANIAK,
    STRILOCHNYK,
    BOMBA;

    public static ERoleOrder fromName(String name) {
        for (ERoleOrder role : values()) {
            if (role.name().equalsIgnoreCase(name)) {
                return role;
            }
        }
        throw new IllegalArgumentException("No enum constant for name: " + name);
    }
}
