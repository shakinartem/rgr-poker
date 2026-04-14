package poker.client;

import poker.common.Protocol;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GameController {
    private final PokerClientFrame frame;
    private final NetworkClient client = new NetworkClient();

    public GameController(PokerClientFrame frame) {
        this.frame = frame;
    }

    public void connect(String host, int port, String name) {
        try {
            client.connect(host, port, this::handleMessage);
            client.send(Map.of("type", Protocol.TYPE_JOIN, "name", name));
            frame.appendStatus("Connected to " + host + ":" + port);
        } catch (IOException exception) {
            frame.appendStatus("Connection failed: " + exception.getMessage());
        }
    }

    public void sendAction(String action, int amount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", Protocol.TYPE_ACTION);
        payload.put("action", action);
        payload.put("amount", amount);
        try {
            client.send(payload);
        } catch (IOException exception) {
            frame.appendStatus("Send failed: " + exception.getMessage());
        }
    }

    private void handleMessage(Map<String, Object> message) {
        SwingUtilities.invokeLater(() -> {
            String type = String.valueOf(message.get("type"));
            switch (type) {
                case Protocol.TYPE_JOIN_OK -> frame.appendStatus("Joined as " + message.get("name"));
                case Protocol.TYPE_JOIN_ERROR, Protocol.TYPE_ERROR -> frame.appendStatus(String.valueOf(message.get("message")));
                case Protocol.TYPE_INFO -> frame.appendStatus(String.valueOf(message.get("message")));
                case Protocol.TYPE_STATE -> frame.renderState(message);
                default -> frame.appendStatus("Unknown message: " + type);
            }
        });
    }
}
