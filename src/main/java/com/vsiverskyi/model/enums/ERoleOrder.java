package com.vsiverskyi.model.enums;

public enum ERoleOrder {
    UNDEFINED("Undefined", ETeam.PEACE),
    MAFIA("Хабарник", ETeam.MAFIA),
    DON("Корупціонер", ETeam.MAFIA),
    OTAMAN("Отаман", ETeam.PEACE),
    SHERYF("Гетьман", ETeam.PEACE),
    LEDY("Діва ночі", ETeam.PEACE),
    DOCTOR("Знахар", ETeam.PEACE),
    MANIAK("Маніяк", ETeam.PEACE),
    STRILOCHNYK("Месник", ETeam.PEACE),
    BOMBA("Писар", ETeam.PEACE),
    ZATYCHKA("Пастор", ETeam.PEACE),
    ZATYCHKA_SUDDYA("Суддя", ETeam.PEACE),
    PEREVERTEN_PEACE("Яничар-мирний", ETeam.PEACE),
    PEREVERTEN_MAFIA("Яничар-мафія", ETeam.MAFIA),
    KRADIY("Крадій", ETeam.PEACE),
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
