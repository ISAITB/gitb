package ecas;

public enum AuthenticationLevel {

    BASIC("BASIC"), MEDIUM("MEDIUM"), HIGH("HIGH");


    private final String levelName;

    AuthenticationLevel(String levelName) {
        this.levelName = levelName;
    }

    public static AuthenticationLevel fromName(String levelName) {
        if (BASIC.levelName.equalsIgnoreCase(levelName)) {
            return BASIC;
        } else if (MEDIUM.levelName.equalsIgnoreCase(levelName)) {
            return MEDIUM;
        } else if (HIGH.levelName.equalsIgnoreCase(levelName)) {
            return HIGH;
        } else {
            throw new IllegalArgumentException("Unknown authentication level [%s]".formatted(levelName));
        }
    }

    @Override
    public String toString() {
        return levelName;
    }
}
