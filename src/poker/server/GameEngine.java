package poker.server;

import poker.common.ActionType;
import poker.common.Card;
import poker.common.GameStage;
import poker.common.HandEvaluator;
import poker.common.HandRank;
import poker.common.Rank;
import poker.common.Suit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public final class GameEngine {
    private static final int SMALL_BLIND = 10;
    private static final int BIG_BLIND = 20;
    private static final long TURN_TIMEOUT_MS = 90_000L;

    private final GameTable table;
    private final Random random = new Random();

    private int dealerIndex = -1;
    private GameStage stage = GameStage.WAITING;
    private int pot;
    private int currentBet;
    private int minRaise;
    private String statusText = "Waiting for players";
    private String currentTurnPlayer = "";
    private long turnDeadlineMillis;
    private final List<String> currentHandPlayers = new ArrayList<>();
    private final List<Card> deck = new ArrayList<>(52);
    private final List<Card> communityCards = new ArrayList<>(5);

    public GameEngine(GameTable table) {
        this.table = table;
    }

    public synchronized void playRound() {
        List<Player> players = table.activePlayersSnapshot();
        if (players.size() < 2) {
            stage = GameStage.WAITING;
            statusText = "Waiting for at least 2 players";
            currentTurnPlayer = "";
            turnDeadlineMillis = 0L;
            currentHandPlayers.clear();
            table.broadcastState();
            return;
        }

        prepareRound(players);
        postBlinds(players);
        broadcast("New round started");
        table.broadcastState();

        if (!bettingRound(players, firstToActPreFlop(players))) {
            return;
        }

        revealCommunity(3, GameStage.FLOP, "Flop");
        if (!bettingRound(players, firstToActPostFlop(players))) {
            return;
        }

        revealCommunity(1, GameStage.TURN, "Turn");
        if (!bettingRound(players, firstToActPostFlop(players))) {
            return;
        }

        revealCommunity(1, GameStage.RIVER, "River");
        if (!bettingRound(players, firstToActPostFlop(players))) {
            return;
        }

        stage = GameStage.SHOWDOWN;
        statusText = "Showdown";
        currentTurnPlayer = "";
        turnDeadlineMillis = 0L;
        finishShowdown(players);
        table.broadcastState();
    }

    public synchronized GameStage stage() {
        return stage;
    }

    public synchronized int pot() {
        return pot;
    }

    public synchronized int currentBet() {
        return currentBet;
    }

    public synchronized int minimumRaiseTo(Player player) {
        int minimumTarget = currentBet + Math.max(minRaise, BIG_BLIND);
        return currentBet == 0 ? Math.max(BIG_BLIND, player.roundContribution() + BIG_BLIND) : minimumTarget;
    }

    public synchronized int maximumRaiseTo(Player player) {
        return player.roundContribution() + player.stack();
    }

    public synchronized String statusText() {
        return statusText;
    }

    public synchronized String currentTurnPlayer() {
        return currentTurnPlayer;
    }

    public synchronized int turnSecondsLeft() {
        if (turnDeadlineMillis <= 0L) {
            return 0;
        }
        long remaining = Math.max(0L, turnDeadlineMillis - System.currentTimeMillis());
        return (int) Math.ceil(remaining / 1000.0);
    }

    public synchronized List<Card> communityCardsSnapshot() {
        return List.copyOf(communityCards);
    }

    public synchronized boolean isCurrentHandPlayer(Player player) {
        return currentHandPlayers.contains(player.name());
    }

    private void prepareRound(List<Player> players) {
        buildDeck();
        communityCards.clear();
        pot = 0;
        currentBet = 0;
        minRaise = BIG_BLIND;
        dealerIndex = (dealerIndex + 1) % players.size();
        stage = GameStage.PRE_FLOP;
        statusText = "Pre-flop";
        currentTurnPlayer = "";
        turnDeadlineMillis = 0L;
        currentHandPlayers.clear();
        players.stream().map(Player::name).forEach(currentHandPlayers::add);

        for (Player player : players) {
            player.resetForRound();
            player.holeCards().add(draw());
            player.holeCards().add(draw());
        }
    }

    private void postBlinds(List<Player> players) {
        int smallBlindIndex = (dealerIndex + 1) % players.size();
        int bigBlindIndex = (dealerIndex + 2) % players.size();
        Player sb = players.get(smallBlindIndex);
        Player bb = players.get(bigBlindIndex);
        pot += sb.bet(SMALL_BLIND);
        pot += bb.bet(BIG_BLIND);
        currentBet = BIG_BLIND;
        sb.setActedThisRound(false);
        bb.setActedThisRound(false);
        broadcast(sb.name() + " posted small blind " + SMALL_BLIND);
        broadcast(bb.name() + " posted big blind " + BIG_BLIND);
    }

    private boolean bettingRound(List<Player> players, int startIndex) {
        resetStreetFlags(players);
        int cursor = startIndex;

        while (true) {
            if (remainingPlayers(players) <= 1) {
                awardWithoutShowdown(players);
                return false;
            }
            if (streetFinished(players)) {
                for (Player player : players) {
                    player.setRoundContribution(0);
                }
                currentBet = 0;
                minRaise = BIG_BLIND;
                currentTurnPlayer = "";
                turnDeadlineMillis = 0L;
                table.broadcastState();
                return true;
            }

            Player player = players.get(cursor);
            cursor = (cursor + 1) % players.size();
            if (!canAct(player)) {
                continue;
            }

            PlayerSession session = player.session();
            session.clearPendingActions();
            currentTurnPlayer = player.name();
            statusText = "Turn: " + player.name();
            turnDeadlineMillis = System.currentTimeMillis() + TURN_TIMEOUT_MS;
            table.broadcastState();
            PlayerSession.PlayerAction action;
            try {
                action = waitForActionWithCountdown(session);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }

            if (action == null) {
                action = autoActionFor(player);
                broadcast(player.name() + " timed out: " + action.type());
            }

            turnDeadlineMillis = 0L;

            if (!applyAction(player, action, players)) {
                player.session().sendError("Action rejected");
                continue;
            }
            table.broadcastState();
        }
    }

    private void resetStreetFlags(List<Player> players) {
        for (Player player : players) {
            player.setActedThisRound(player.allIn() || player.folded());
        }
    }

    private boolean streetFinished(List<Player> players) {
        for (Player player : players) {
            if (player.folded() || player.allIn()) {
                continue;
            }
            if (!player.actedThisRound() || player.roundContribution() != currentBet) {
                return false;
            }
        }
        return true;
    }

    private boolean canAct(Player player) {
        return !player.folded() && !player.allIn() && !player.sittingOut();
    }

    private PlayerSession.PlayerAction autoActionFor(Player player) {
        int toCall = Math.max(0, currentBet - player.roundContribution());
        if (toCall == 0) {
            return new PlayerSession.PlayerAction(ActionType.CHECK, 0);
        }
        return new PlayerSession.PlayerAction(ActionType.FOLD, 0);
    }

    private boolean applyAction(Player player, PlayerSession.PlayerAction action, List<Player> players) {
        int toCall = Math.max(0, currentBet - player.roundContribution());
        switch (action.type()) {
            case FOLD -> {
                player.setFolded(true);
                player.setActedThisRound(true);
                broadcast(player.name() + " folds");
                return true;
            }
            case CHECK -> {
                if (toCall != 0) {
                    return false;
                }
                player.setActedThisRound(true);
                broadcast(player.name() + " checks");
                return true;
            }
            case CALL -> {
                if (toCall == 0) {
                    return false;
                }
                if (player.stack() > toCall && action.amount() > 0 && action.amount() != toCall) {
                    return false;
                }
                int added = player.bet(toCall);
                pot += added;
                player.setActedThisRound(true);
                broadcast(player.name() + " calls " + added);
                return true;
            }
            case ALL_IN -> {
                if (player.stack() <= 0) {
                    return false;
                }
                int before = player.roundContribution();
                int added = player.bet(player.stack());
                pot += added;
                player.setActedThisRound(true);
                if (player.roundContribution() > currentBet) {
                    int raiseAmount = player.roundContribution() - currentBet;
                    currentBet = player.roundContribution();
                    minRaise = Math.max(minRaise, raiseAmount);
                    reopenBetting(players, player);
                }
                broadcast(player.name() + " goes all-in for " + (before + added));
                return true;
            }
            case RAISE -> {
                int raiseTo = action.amount();
                int minimumTarget = currentBet + Math.max(minRaise, BIG_BLIND);
                if (raiseTo <= currentBet || raiseTo > player.roundContribution() + player.stack()) {
                    return false;
                }
                if (raiseTo < minimumTarget && raiseTo != player.roundContribution() + player.stack()) {
                    return false;
                }
                int added = player.bet(raiseTo - player.roundContribution());
                pot += added;
                int raiseAmount = raiseTo - currentBet;
                currentBet = player.roundContribution();
                minRaise = Math.max(BIG_BLIND, raiseAmount);
                player.setActedThisRound(true);
                reopenBetting(players, player);
                broadcast(player.name() + " raises to " + currentBet);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void reopenBetting(List<Player> players, Player raiser) {
        for (Player player : players) {
            if (player == raiser || player.folded() || player.allIn()) {
                continue;
            }
            player.setActedThisRound(false);
        }
        raiser.setActedThisRound(true);
    }

    private void revealCommunity(int count, GameStage nextStage, String label) {
        stage = nextStage;
        for (int i = 0; i < count; i++) {
            communityCards.add(draw());
        }
        statusText = label;
        currentTurnPlayer = "";
        turnDeadlineMillis = 0L;
        broadcast(label + ": " + communityCards.stream().map(Card::code).collect(Collectors.joining(" ")));
        table.broadcastState();
    }

    private void awardWithoutShowdown(List<Player> players) {
        Player winner = players.stream().filter(player -> !player.folded()).findFirst().orElse(null);
        if (winner == null) {
            return;
        }
        winner.setStack(winner.stack() + pot);
        broadcast(winner.name() + " wins pot " + pot + " without showdown");
        pot = 0;
        stage = GameStage.WAITING;
        statusText = "Round finished";
        currentTurnPlayer = "";
        turnDeadlineMillis = 0L;
        currentHandPlayers.clear();
    }

    private void finishShowdown(List<Player> players) {
        Map<Player, HandRank> ranks = new LinkedHashMap<>();
        for (Player player : players) {
            if (player.folded()) {
                continue;
            }
            List<Card> cards = new ArrayList<>(communityCards);
            cards.addAll(player.holeCards());
            HandRank rank = HandEvaluator.evaluateSeven(cards);
            ranks.put(player, rank);
            broadcast(player.name() + ": " + rank.description());
        }

        List<PotSlice> slices = buildPotSlices(players);
        for (PotSlice slice : slices) {
            List<Player> eligible = slice.eligible().stream().filter(player -> !player.folded()).toList();
            if (eligible.isEmpty()) {
                continue;
            }
            HandRank best = eligible.stream().map(ranks::get).max(Comparator.naturalOrder()).orElseThrow();
            List<Player> winners = eligible.stream().filter(player -> ranks.get(player).compareTo(best) == 0).toList();
            int share = slice.amount() / winners.size();
            int remainder = slice.amount() % winners.size();
            for (int i = 0; i < winners.size(); i++) {
                Player winner = winners.get(i);
                int payout = share + (i < remainder ? 1 : 0);
                winner.setStack(winner.stack() + payout);
            }
            broadcast("Pot " + slice.amount() + " won by " +
                    winners.stream().map(Player::name).collect(Collectors.joining(", ")) +
                    " with " + best.description());
        }
        pot = 0;
        stage = GameStage.WAITING;
        statusText = "Round finished";
        currentTurnPlayer = "";
        turnDeadlineMillis = 0L;
        currentHandPlayers.clear();
    }

    private List<PotSlice> buildPotSlices(List<Player> players) {
        List<Integer> levels = players.stream()
                .map(Player::totalContribution)
                .filter(value -> value > 0)
                .distinct()
                .sorted()
                .toList();
        List<PotSlice> slices = new ArrayList<>();
        int previous = 0;
        for (int level : levels) {
            int delta = level - previous;
            List<Player> contributors = players.stream()
                    .filter(player -> player.totalContribution() >= level)
                    .toList();
            int amount = contributors.size() * delta;
            if (amount > 0) {
                slices.add(new PotSlice(amount, contributors));
            }
            previous = level;
        }
        return slices;
    }

    private int remainingPlayers(List<Player> players) {
        return (int) players.stream().filter(player -> !player.folded()).count();
    }

    private int firstToActPreFlop(List<Player> players) {
        return players.size() == 2 ? dealerIndex : (dealerIndex + 3) % players.size();
    }

    private int firstToActPostFlop(List<Player> players) {
        return (dealerIndex + 1) % players.size();
    }

    private void buildDeck() {
        deck.clear();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                deck.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(deck, random);
    }

    private Card draw() {
        return deck.remove(deck.size() - 1);
    }

    private void broadcast(String message) {
        table.log(message);
    }

    private PlayerSession.PlayerAction waitForActionWithCountdown(PlayerSession session) throws InterruptedException {
        while (true) {
            long remaining = turnDeadlineMillis - System.currentTimeMillis();
            if (remaining <= 0L) {
                return null;
            }
            PlayerSession.PlayerAction action = session.awaitAction(Math.min(1_000L, remaining));
            if (action != null) {
                return action;
            }
            table.broadcastState();
        }
    }

    private record PotSlice(int amount, List<Player> eligible) {
    }
}
