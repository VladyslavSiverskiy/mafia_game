package com.vsiverskyi.model.enums;

public enum ERoleOrder {
    UNDEFINED("Undefined", ETeam.PEACE),
    MAFIA("Хабарник", ETeam.MAFIA),
    DON("Корупціонер", ETeam.MAFIA),
    PEREVERTEN_MAFIA("Яничар-корупціонер", ETeam.MAFIA),
    SHERYF("Гетьман", ETeam.PEACE),
    OTAMAN("Отаман", ETeam.PEACE),
    LEDY("Діва ночі", ETeam.PEACE),
    MANIAK("Маніяк", ETeam.PEACE),
    ZATYCHKA("Пастор", ETeam.PEACE),
    ZATYCHKA_SUDDYA("Суддя", ETeam.PEACE),
    KRADIY("Крадій", ETeam.PEACE),
    DOCTOR("Знахар", ETeam.PEACE),
    STRILOCHNYK("Месник", ETeam.PEACE),
    BOMBA("Писар", ETeam.PEACE),
    PEREVERTEN_PEACE("Яничар-мирний", ETeam.PEACE),
    PEACE("Козак", ETeam.PEACE);

    ERoleOrder(final String title, final ETeam eTeam) {
        this.title = title;
        this.eTeam = eTeam;
    }

    private String title;
    private ETeam eTeam;

    public String getTitle() {
        return title;
    }

    public ETeam getTeam() {
        return eTeam;
    }

    public static ERoleOrder fromName(String name) {
        for (ERoleOrder role : values()) {
            if (role.name().equalsIgnoreCase(name)) {
                return role;
            }
        }
        return UNDEFINED;
    }

    @Override
    public String toString() {
        return "ERoleOrder{" +
               "title='" + title + '\'' +
               ", eTeam=" + eTeam +
               "} " + super.toString();
    }
}
