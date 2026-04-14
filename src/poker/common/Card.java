package poker.common;

import java.util.Objects;

public final class Card {
    private final Suit suit;
    private final Rank rank;

    public Card(Suit suit, Rank rank) {
        this.suit = Objects.requireNonNull(suit);
        this.rank = Objects.requireNonNull(rank);
    }

    public Suit suit() {
        return suit;
    }

    public Rank rank() {
        return rank;
    }

    public String code() {
        return rank.shortName() + suit.shortName();
    }

    @Override
    public String toString() {
        return code();
    }
}
