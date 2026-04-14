package poker.server;

import poker.common.ActionType;
import poker.common.Card;
import poker.common.GameStage;
import poker.common.Protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class GameTable {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");

    private final List<Player> players = new CopyOnWriteArrayList<>();
    private final List<String> logMessages = new ArrayList<>();
    private final GameEngine engine = new GameEngine(this);
    private final Thread gameLoop;

    public GameTable() {
        gameLoop = new Thread(this::loop, "game-loop");
        gameLoop.setDaemon(true);
        gameLoop.start();
    }

    public synchronized Player registerPlayer(PlayerSession session, String requestedName) {
        if (!NAME_PATTERN.matcher(requestedName).matches()) {
            session.sendError("Name must match ^[a-zA-Z0-9_]{3,16}$");
            return null;
        }
        if (players.size() >= 4) {
            session.send(Map.of("type", Protocol.TYPE_JOIN_ERROR, "message", "Table is full"));
            return null;
        }
        boolean taken = players.stream().anyMatch(player -> player.name().equalsIgnoreCase(requestedName));
        if (taken) {
            session.send(Map.of("type", Protocol.TYPE_JOIN_ERROR, "message", "Name already used"));
            return null;
        }
        Player player = new Player(requestedName, session);
        players.add(player);
        log(player.name() + " joined the table");
        broadcastState();
        return player;
    }

    public synchronized void disconnect(PlayerSession session) {
        Player player = session.player();
        if (player == null) {
            return;
        }
        player.setSittingOut(true);
        player.setFolded(true);
        log(player.name() + " disconnected");
        broadcastState();
    }

    public synchronized List<Player> activePlayersSnapshot() {
        return players.stream()
                .filter(player -> player.isConnected() && player.stack() > 0)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public synchronized void log(String message) {
        logMessages.add(message);
        if (logMessages.size() > 40) {
            logMessages.remove(0);
        }
        broadcastState();
    }

    public synchronized void broadcastState() {
        for (Player viewer : players) {
            if (!viewer.session().isConnected()) {
                continue;
            }
            viewer.session().send(buildStateFor(viewer));
        }
    }

    private Map<String, Object> buildStateFor(Player viewer) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", Protocol.TYPE_STATE);
        payload.put("stage", engine.stage().name());
        payload.put("pot", engine.pot());
        payload.put("currentBet", engine.currentBet());
        payload.put("currentTurn", engine.currentTurnPlayer());
        payload.put("turnSecondsLeft", engine.turnSecondsLeft());
        payload.put("status", engine.statusText());
        payload.put("community", engine.communityCardsSnapshot().stream().map(Card::code).toList());
        payload.put("players", players.stream().map(player -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", player.name());
            item.put("stack", player.stack());
            item.put("roundBet", player.roundContribution());
            item.put("totalBet", player.totalContribution());
            item.put("folded", player.folded());
            item.put("allIn", player.allIn());
            item.put("connected", player.isConnected());
            boolean showCards = player == viewer || (engine.stage() == GameStage.SHOWDOWN && !player.folded());
            item.put("cards", showCards ? player.holeCards().stream().map(Card::code).toList() : List.of());
            return item;
        }).toList());
        payload.put("you", viewer.name());
        payload.put("holeCards", viewer.holeCards().stream().map(Card::code).toList());
        payload.put("allowedActions", allowedActionsFor(viewer));
        payload.put("log", List.copyOf(logMessages));
        return payload;
    }

    private List<String> allowedActionsFor(Player player) {
        if (engine.stage() == GameStage.WAITING || player.folded() || player.allIn() || player.sittingOut()) {
            return List.of();
        }
        if (!player.name().equals(engine.currentTurnPlayer())) {
            return List.of();
        }
        int toCall = Math.max(0, engine.currentBet() - player.roundContribution());
        List<String> actions = new ArrayList<>();
        actions.add(ActionType.FOLD.name());
        if (toCall == 0) {
            actions.add(ActionType.CHECK.name());
        } else {
            actions.add(ActionType.CALL.name());
        }
        if (player.stack() > toCall) {
            actions.add(ActionType.RAISE.name());
        }
        if (player.stack() > 0) {
            actions.add(ActionType.ALL_IN.name());
        }
        return actions;
    }

    private void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (activePlayersSnapshot().size() >= 2 && engine.stage() == GameStage.WAITING) {
                    engine.playRound();
                    Thread.sleep(2_000L);
                } else {
                    broadcastState();
                    Thread.sleep(1_000L);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception exception) {
                log("Server error: " + exception.getMessage());
            }
        }
    }
}
