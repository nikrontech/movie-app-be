package lt.rildev.movieappbe.model;

public enum City {
    VILNIUS,
    KLAIPEDA,
    KAUNAS,
    SIAULIAI;

    public static City fromForumCinemaName(String name) {
        if (name == null) return null;
        return switch (name) {
            case "Klaipėda" -> KLAIPEDA;
            case "Šiauliai" -> SIAULIAI;
            case "Vingis" -> VILNIUS;
            case "Kaunas" -> KAUNAS;
            default -> null;
        };
    }
}
