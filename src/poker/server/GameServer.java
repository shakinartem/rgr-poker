package poker.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GameServer {
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        ExecutorService pool = Executors.newCachedThreadPool();
        GameTable table = new GameTable();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Poker server started on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                PlayerSession session = new PlayerSession(socket, table);
                pool.submit(session);
            }
        }
    }
}
