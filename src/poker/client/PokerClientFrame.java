package poker.client;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PokerClientFrame extends JFrame {
    private final JTextField hostField = new JTextField("127.0.0.1", 10);
    private final JTextField portField = new JTextField("5000", 5);
    private final JTextField nameField = new JTextField("Player1", 10);
    private final JButton connectButton = new JButton("Connect");

    private final JLabel stageLabel = new JLabel("Stage: WAITING");
    private final JLabel potLabel = new JLabel("Pot: 0");
    private final JLabel turnLabel = new JLabel("Turn: -");
    private final JLabel timerLabel = new JLabel("Time left: 0 s");
    private final JLabel statusLabel = new JLabel("Status: not connected");
    private final JPanel communityPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
    private final SeatPanel[] seatPanels = {
            new SeatPanel(), new SeatPanel(), new SeatPanel(), new SeatPanel()
    };
    private final JTextArea logArea = new JTextArea();

    private final JButton foldButton = new JButton("Fold");
    private final JButton checkButton = new JButton("Check");
    private final JButton callButton = new JButton("Call");
    private final JButton raiseButton = new JButton("Raise");
    private final JButton allInButton = new JButton("All-in");
    private final JSpinner raiseSpinner = new JSpinner(new SpinnerNumberModel(40, 1, 10_000, 10));

    private final GameController controller;

    public PokerClientFrame() {
        super("Texas Hold'em Client");
        this.controller = new GameController(this);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        setPreferredSize(new Dimension(1200, 780));
        pack();
        setLocationRelativeTo(null);
        wireActions();
        setActionButtonsEnabled(List.of());
    }

    @SuppressWarnings("unchecked")
    public void renderState(Map<String, Object> state) {
        stageLabel.setText("Stage: " + state.get("stage"));
        potLabel.setText("Pot: " + state.get("pot") + " | Current bet: " + state.get("currentBet"));
        turnLabel.setText("Turn: " + state.get("currentTurn"));
        timerLabel.setText("Time left: " + state.get("turnSecondsLeft") + " s");
        statusLabel.setText("Status: " + state.get("status"));
        renderCommunity((List<Object>) state.get("community"));
        renderPlayers((List<Object>) state.get("players"), String.valueOf(state.get("you")));
        logArea.setText(((List<Object>) state.get("log")).stream().map(String::valueOf).collect(Collectors.joining("\n")));
        List<String> allowed = ((List<Object>) state.get("allowedActions")).stream().map(String::valueOf).toList();
        updateRaiseSpinner(state, allowed);
        setActionButtonsEnabled(allowed);
    }

    public void appendStatus(String text) {
        statusLabel.setText("Status: " + text);
    }

    public void setConnectEnabled(boolean enabled) {
        connectButton.setEnabled(enabled);
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Host"));
        panel.add(hostField);
        panel.add(new JLabel("Port"));
        panel.add(portField);
        panel.add(new JLabel("Name"));
        panel.add(nameField);
        panel.add(connectButton);
        return panel;
    }

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(buildTablePanel(), BorderLayout.CENTER);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(320, 0));
        logScroll.setBorder(BorderFactory.createTitledBorder("Game Log"));
        panel.add(logScroll, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildTablePanel() {
        JPanel table = new JPanel(new GridBagLayout());
        table.setBackground(new Color(24, 94, 52));
        table.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(107, 71, 40), 8),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));

        communityPanel.setOpaque(false);

        JPanel centerInfo = new JPanel();
        centerInfo.setOpaque(false);
        centerInfo.setLayout(new BoxLayout(centerInfo, BoxLayout.Y_AXIS));
        stageLabel.setForeground(Color.WHITE);
        potLabel.setForeground(Color.WHITE);
        turnLabel.setForeground(Color.WHITE);
        timerLabel.setForeground(new Color(255, 235, 140));
        statusLabel.setForeground(Color.WHITE);
        stageLabel.setAlignmentX(CENTER_ALIGNMENT);
        potLabel.setAlignmentX(CENTER_ALIGNMENT);
        turnLabel.setAlignmentX(CENTER_ALIGNMENT);
        timerLabel.setAlignmentX(CENTER_ALIGNMENT);
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        communityPanel.setAlignmentX(CENTER_ALIGNMENT);
        centerInfo.add(stageLabel);
        centerInfo.add(potLabel);
        centerInfo.add(turnLabel);
        centerInfo.add(timerLabel);
        centerInfo.add(statusLabel);
        centerInfo.add(communityPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);

        gbc.gridx = 1;
        gbc.gridy = 0;
        table.add(seatPanels[1], gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        table.add(seatPanels[2], gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        table.add(centerInfo, gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        table.add(seatPanels[3], gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        table.add(seatPanels[0], gbc);

        return table;
    }

    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(foldButton);
        panel.add(checkButton);
        panel.add(callButton);
        panel.add(raiseButton);
        panel.add(new JLabel("Raise to"));
        panel.add(raiseSpinner);
        panel.add(allInButton);
        return panel;
    }

    private void wireActions() {
        connectButton.addActionListener(event -> {
            String host = hostField.getText().trim();
            String portText = portField.getText().trim();
            String name = nameField.getText().trim();
            try {
                int port = Integer.parseInt(portText);
                connectButton.setEnabled(false);
                controller.connect(host, port, name);
            } catch (NumberFormatException exception) {
                connectButton.setEnabled(true);
                JOptionPane.showMessageDialog(this, "Port must be a number", "Validation", JOptionPane.WARNING_MESSAGE);
            }
        });
        foldButton.addActionListener(event -> controller.sendAction("FOLD", 0));
        checkButton.addActionListener(event -> controller.sendAction("CHECK", 0));
        callButton.addActionListener(event -> controller.sendAction("CALL", 0));
        raiseButton.addActionListener(event -> controller.sendAction("RAISE", (Integer) raiseSpinner.getValue()));
        allInButton.addActionListener(event -> controller.sendAction("ALL_IN", 0));
    }

    private void setActionButtonsEnabled(List<String> allowed) {
        foldButton.setEnabled(allowed.contains("FOLD"));
        checkButton.setEnabled(allowed.contains("CHECK"));
        callButton.setEnabled(allowed.contains("CALL"));
        raiseButton.setEnabled(allowed.contains("RAISE"));
        allInButton.setEnabled(allowed.contains("ALL_IN"));
        raiseSpinner.setEnabled(allowed.contains("RAISE"));
    }

    private void updateRaiseSpinner(Map<String, Object> state, List<String> allowed) {
        int minimum = ((Number) state.getOrDefault("raiseMin", 20)).intValue();
        int maximum = ((Number) state.getOrDefault("raiseMax", 10_000)).intValue();
        if (maximum < minimum) {
            maximum = minimum;
        }
        int current = raiseSpinner.getValue() instanceof Number number ? number.intValue() : minimum;
        int next = Math.max(minimum, Math.min(current, maximum));
        SpinnerModel model = new SpinnerNumberModel(next, minimum, maximum, 10);
        raiseSpinner.setModel(model);
        if (!allowed.contains("RAISE")) {
            raiseSpinner.setValue(minimum);
        }
    }

    private void renderCommunity(List<Object> cards) {
        communityPanel.removeAll();
        List<String> codes = cards.stream().map(String::valueOf).toList();
        int cardCount = Math.max(5, codes.size());
        for (int index = 0; index < cardCount; index++) {
            String code = index < codes.size() ? codes.get(index) : "BACK";
            communityPanel.add(new JLabel(CardImageCache.load(code)));
        }
        communityPanel.revalidate();
        communityPanel.repaint();
    }

    @SuppressWarnings("unchecked")
    private void renderPlayers(List<Object> players, String youName) {
        List<Map<String, Object>> ordered = new ArrayList<>();
        for (Object item : players) {
            ordered.add((Map<String, Object>) item);
        }

        int yourIndex = 0;
        for (int index = 0; index < ordered.size(); index++) {
            if (String.valueOf(ordered.get(index).get("name")).equals(youName)) {
                yourIndex = index;
                break;
            }
        }

        int[] seatToOffset = {0, 1, 2, 3};
        for (int seat = 0; seat < seatPanels.length; seat++) {
            int offset = seatToOffset[seat];
            if (offset < ordered.size()) {
                Map<String, Object> player = ordered.get((yourIndex + offset) % ordered.size());
                List<String> cards = ((List<Object>) player.get("cards")).stream().map(String::valueOf).toList();
                seatPanels[seat].render(
                        String.valueOf(player.get("name")),
                        ((Number) player.get("stack")).intValue(),
                        ((Number) player.get("roundBet")).intValue(),
                        Boolean.TRUE.equals(player.get("folded")),
                        Boolean.TRUE.equals(player.get("allIn")),
                        Boolean.TRUE.equals(player.get("connected")),
                        Boolean.TRUE.equals(player.get("waiting")),
                        cards);
            } else {
                seatPanels[seat].clear();
            }
        }
    }
}
