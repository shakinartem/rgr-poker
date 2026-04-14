package poker.common;

public enum Suit {
    CLUBS("C"),
    DIAMONDS("D"),
    HEARTS("H"),
    SPADES("S");

    private final String shortName;

    Suit(String shortName) {
        this.shortName = shortName;
    }

    public String shortName() {
        return shortName;
    }
}
