package poker.server;

import poker.common.Card;

import java.util.ArrayList;
import java.util.List;

public final class Player {
    private final String name;
    private final PlayerSession session;
    private final List<Card> holeCards = new ArrayList<>(2);
    private int stack = 1_000;
    private int totalContribution;
    private int roundContribution;
    private boolean folded;
    private boolean allIn;
    private boolean sittingOut;
    private boolean actedThisRound;

    public Player(String name, PlayerSession session) {
        this.name = name;
        this.session = session;
    }

    public String name() {
        return name;
    }

    public PlayerSession session() {
        return session;
    }

    public List<Card> holeCards() {
        return holeCards;
    }

    public int stack() {
        return stack;
    }

    public void setStack(int stack) {
        this.stack = stack;
    }

    public int totalContribution() {
        return totalContribution;
    }

    public void setTotalContribution(int totalContribution) {
        this.totalContribution = totalContribution;
    }

    public int roundContribution() {
        return roundContribution;
    }

    public void setRoundContribution(int roundContribution) {
        this.roundContribution = roundContribution;
    }

    public boolean folded() {
        return folded;
    }

    public void setFolded(boolean folded) {
        this.folded = folded;
    }

    public boolean allIn() {
        return allIn;
    }

    public void setAllIn(boolean allIn) {
        this.allIn = allIn;
    }

    public boolean sittingOut() {
        return sittingOut;
    }

    public void setSittingOut(boolean sittingOut) {
        this.sittingOut = sittingOut;
    }

    public boolean actedThisRound() {
        return actedThisRound;
    }

    public void setActedThisRound(boolean actedThisRound) {
        this.actedThisRound = actedThisRound;
    }

    public boolean isConnected() {
        return !sittingOut && session.isConnected();
    }

    public void resetForRound() {
        holeCards.clear();
        totalContribution = 0;
        roundContribution = 0;
        folded = false;
        allIn = false;
        actedThisRound = false;
        sittingOut = !session.isConnected() || stack <= 0;
    }

    public int bet(int amount) {
        int actual = Math.min(amount, stack);
        stack -= actual;
        totalContribution += actual;
        roundContribution += actual;
        if (stack == 0) {
            allIn = true;
        }
        return actual;
    }
}
