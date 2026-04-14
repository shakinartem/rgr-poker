package poker.client;

import poker.common.Protocol;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GameController {
    private final PokerClientFrame frame;
    private NetworkClient client;
    private boolean transportConnected;
    private boolean joined;

    public GameController(PokerClientFrame frame) {
        this.frame = frame;
    }

    public void connect(String host, int port, String name) {
        try {
            if (!transportConnected) {
                client = new NetworkClient();
                client.connect(host, port, this::handleMessage);
                transportConnected = true;
            }
            client.send(Map.of("type", Protocol.TYPE_JOIN, "name", name));
            frame.appendStatus("Подключение к " + host + ":" + port);
        } catch (IOException exception) {
            resetConnection();
            frame.appendStatus("Ошибка подключения: " + exception.getMessage());
            frame.setConnectEnabled(true);
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
            frame.appendStatus("Ошибка отправки: " + exception.getMessage());
        }
    }

    private void handleMessage(Map<String, Object> message) {
        SwingUtilities.invokeLater(() -> {
            String type = String.valueOf(message.get("type"));
            switch (type) {
                case Protocol.TYPE_JOIN_OK -> {
                    joined = true;
                    frame.appendStatus("Вы вошли как " + message.get("name"));
                    frame.setConnectEnabled(false);
                }
                case Protocol.TYPE_JOIN_ERROR -> {
                    frame.appendStatus(String.valueOf(message.get("message")));
                    frame.setConnectEnabled(true);
                }
                case Protocol.TYPE_ERROR -> {
                    frame.appendStatus(String.valueOf(message.get("message")));
                    if (!joined) {
                        frame.setConnectEnabled(true);
                    }
                }
                case Protocol.TYPE_INFO -> frame.appendStatus(String.valueOf(message.get("message")));
                case Protocol.TYPE_STATE -> frame.renderState(message);
                default -> frame.appendStatus("Неизвестное сообщение: " + type);
            }
        });
    }

    private void resetConnection() {
        joined = false;
        transportConnected = false;
        if (client != null) {
            try {
                client.close();
            } catch (IOException ignored) {
            }
            client = null;
        }
    }
}
