package com.vsiverskyi.model.enums;

public enum ERoleOrder {
    UNDEFINED("Undefined"),
    MAFIA("Хабарник"),
    DON("Корупціонер"),
    OTAMAN("Отаман"),
    SHERYF("Гетьман"),
    DOCTOR("Знахар"),
    LEDY("Діва ночі"),
    MANIAK("Маніяк"),
    STRILOCHNYK("Месник"),
    BOMBA("Писар"),
    ZATYCHKA("Пастор"),
    ZATYCHKA_SUDDYA("Суддя"),
    PEREVERTEN_PEACE("Яничар-мирний"),
    PEREVERTEN_MAFIA("Яничар-мафія"),
    KRADIY("Крадій"),
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
