package lt.rildev.movieappbe.task;

public enum Cinema {

    PASAKA("Pasaka"),
    VUE("VUE"),
    FORUM("Forum Cinemas"),
    APOLLO("Apollo Kinas");

    private final String displayName;

    Cinema(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
