package poker.client;

import poker.common.JsonUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

public final class NetworkClient implements Closeable {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Thread listenerThread;

    public void connect(String host, int port, Consumer<Map<String, Object>> onMessage) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        listenerThread = new Thread(() -> listen(onMessage), "client-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public synchronized void send(Map<String, Object> payload) throws IOException {
        writer.write(JsonUtil.stringify(payload));
        writer.write('\n');
        writer.flush();
    }

    private void listen(Consumer<Map<String, Object>> onMessage) {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                onMessage.accept(JsonUtil.parseObject(line));
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public void close() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }
}
