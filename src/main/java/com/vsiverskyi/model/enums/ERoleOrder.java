package com.vsiverskyi.model.enums;

public enum ERoleOrder {
    UNDEFINED("Undefined"),
    MAFIA("Хабарник"),
    DON("Корупціонер"),
    SHERYF("Гетьман"),
    DOCTOR("Лікар"),
    LEDY("Леді"),
    MANIAK("Навіжений"),
    STRILOCHNYK("Кармалюк"),
    BOMBA("Бомба"),
    ZATYCHKA("Затичка"),
    PEACE("Козак");

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
