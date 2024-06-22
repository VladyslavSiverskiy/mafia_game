package com.vsiverskyi.model.enums;

/**
 * Енам який буде відповідати за команду гравця, щоб можна було
 * робити порівняння сторін (наприклад 2 мафії і два мирні) - тоді поразка мирних
 */
public enum ETeam {
    MAFIA("Мафія"),
    PEACE("Мирні");

    private String title;

    ETeam(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
