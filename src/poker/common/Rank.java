package poker.common;

public enum Rank {
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "T"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K"),
    ACE(14, "A");

    private final int value;
    private final String shortName;

    Rank(int value, String shortName) {
        this.value = value;
        this.shortName = shortName;
    }

    public int value() {
        return value;
    }

    public String shortName() {
        return shortName;
    }
}
