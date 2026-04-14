package poker.client;

import javax.swing.SwingUtilities;

public final class PokerClient {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PokerClientFrame().setVisible(true));
    }
}
