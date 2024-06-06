package com.vsiverskyi.model.enums;

public enum ERoleOrder {
    UNDEFINED("Undefined"),
    DON("Дон"),
    MAFIA("Мафія"),
    SHERYF("Шериф"),
    DOCTOR("Лікар"),
    LEDY("Леді"),
    MANIAK("Маніяк"),
    STRILOCHNYK("Стрілочник"),
    BOMBA("Бомба"),
    ZATYCHKA("Затичка"),
    PEACE("Мирний");

    ERoleOrder(final String title) {
        this.title = title;
    }

    private String title;

    public String getTitle() {
        return title;
    }

    public static ERoleOrder fromName(String name) {
        for (ERoleOrder role : values()) {
            if (role.name().equalsIgnoreCase(name)) {
                return role;
            }
        }
        return UNDEFINED;
    }
}
