package poker.server;

import poker.common.ActionType;
import poker.common.JsonUtil;
import poker.common.Protocol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class PlayerSession implements Runnable, Closeable {
    public record PlayerAction(ActionType type, int amount) {
    }

    private final Socket socket;
    private final GameTable table;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final BlockingQueue<PlayerAction> actions = new LinkedBlockingQueue<>();
    private volatile boolean connected = true;
    private volatile Player player;

    public PlayerSession(Socket socket, GameTable table) throws IOException {
        this.socket = socket;
        this.table = table;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public boolean isConnected() {
        return connected && !socket.isClosed();
    }

    public Player player() {
        return player;
    }

    public PlayerAction awaitAction(long timeoutMillis) throws InterruptedException {
        return actions.poll(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public void clearPendingActions() {
        actions.clear();
    }

    public synchronized void send(Map<String, Object> payload) {
        if (!isConnected()) {
            return;
        }
        try {
            writer.write(JsonUtil.stringify(payload));
            writer.write('\n');
            writer.flush();
        } catch (IOException exception) {
            connected = false;
            table.disconnect(this);
        }
    }

    public void sendInfo(String text) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", Protocol.TYPE_INFO);
        payload.put("message", text);
        send(payload);
    }

    public void sendError(String text) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", Protocol.TYPE_ERROR);
        payload.put("message", text);
        send(payload);
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                Map<String, Object> message = JsonUtil.parseObject(line);
                String type = String.valueOf(message.get("type"));
                if (Protocol.TYPE_JOIN.equals(type)) {
                    handleJoin(message);
                } else if (Protocol.TYPE_ACTION.equals(type)) {
                    handleAction(message);
                }
            }
        } catch (Exception ignored) {
        } finally {
            connected = false;
            table.disconnect(this);
            try {
                close();
            } catch (IOException ignored) {
            }
        }
    }

    private void handleJoin(Map<String, Object> message) {
        if (player != null) {
            sendError("Игрок уже подключен");
            return;
        }
        String requestedName = String.valueOf(message.getOrDefault("name", "")).trim();
        Player registered = table.registerPlayer(this, requestedName);
        if (registered == null) {
            return;
        }
        this.player = registered;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", Protocol.TYPE_JOIN_OK);
        response.put("name", registered.name());
        send(response);
    }

    private void handleAction(Map<String, Object> message) {
        if (player == null) {
            sendError("Сначала нужно подключиться");
            return;
        }
        try {
            ActionType actionType = ActionType.valueOf(String.valueOf(message.get("action")));
            int amount = ((Number) message.getOrDefault("amount", 0)).intValue();
            actions.offer(new PlayerAction(actionType, amount));
        } catch (Exception exception) {
            sendError("Некорректное действие");
        }
    }

    @Override
    public void close() throws IOException {
        connected = false;
        socket.close();
    }
}
