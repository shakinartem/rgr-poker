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
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PokerClientFrame extends JFrame {
    private static final String[] COMBINATION_ORDER = {
            "Роял-флеш",
            "Стрит-флеш",
            "Каре",
            "Фулл-хаус",
            "Флеш",
            "Стрит",
            "Сет",
            "Две пары",
            "Пара",
            "Старшая карта"
    };

    private final JTextField hostField = new JTextField("127.0.0.1", 10);
    private final JTextField portField = new JTextField("5000", 5);
    private final JTextField nameField = new JTextField("Player1", 10);
    private final JButton connectButton = new JButton("Подключиться");

    private final JLabel stageLabel = new JLabel("Стадия: ожидание");
    private final JLabel potLabel = new JLabel("Банк: 0");
    private final JLabel turnLabel = new JLabel("Ход: -");
    private final JLabel timerLabel = new JLabel("Осталось времени: 0 с");
    private final JLabel statusLabel = new JLabel("Статус: не подключено");
    private final JPanel communityPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
    private final SeatPanel[] seatPanels = {
            new SeatPanel(), new SeatPanel(), new SeatPanel(), new SeatPanel()
    };
    private final JTextArea logArea = new JTextArea();

    private final JButton foldButton = new JButton("Пас");
    private final JButton checkButton = new JButton("Чек");
    private final JButton callButton = new JButton("Колл");
    private final JButton raiseButton = new JButton("Рейз");
    private final JButton allInButton = new JButton("Ва-банк");
    private final JButton myCombinationButton = new JButton("Моя комбинация");
    private final JSpinner raiseSpinner = new JSpinner(new SpinnerNumberModel(40, 1, 10_000, 10));

    private final GameController controller;
    private String currentCombination = "";

    public PokerClientFrame() {
        super("Техасский Холдем");
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
        stageLabel.setText("Стадия: " + translateStage(String.valueOf(state.get("stage"))));
        potLabel.setText("Банк: " + state.get("pot") + " | Текущая ставка: " + state.get("currentBet"));
        turnLabel.setText("Ход: " + state.get("currentTurn"));
        timerLabel.setText("Осталось времени: " + state.get("turnSecondsLeft") + " с");
        statusLabel.setText("Статус: " + state.get("status"));
        currentCombination = String.valueOf(state.getOrDefault("currentCombo", ""));
        renderCommunity((List<Object>) state.get("community"));
        renderPlayers((List<Object>) state.get("players"), String.valueOf(state.get("you")));
        logArea.setText(((List<Object>) state.get("log")).stream().map(String::valueOf).collect(Collectors.joining("\n")));
        List<String> allowed = ((List<Object>) state.get("allowedActions")).stream().map(String::valueOf).toList();
        updateRaiseSpinner(state, allowed);
        setActionButtonsEnabled(allowed);
    }

    public void appendStatus(String text) {
        statusLabel.setText("Статус: " + text);
    }

    public void setConnectEnabled(boolean enabled) {
        connectButton.setEnabled(enabled);
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Хост"));
        panel.add(hostField);
        panel.add(new JLabel("Порт"));
        panel.add(portField);
        panel.add(new JLabel("Имя"));
        panel.add(nameField);
        panel.add(connectButton);
        return panel;
    }

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(buildTablePanel(), BorderLayout.CENTER);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(320, 0));
        logScroll.setBorder(BorderFactory.createTitledBorder("Журнал игры"));
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
        panel.add(new JLabel("Рейз до"));
        panel.add(raiseSpinner);
        panel.add(allInButton);
        panel.add(myCombinationButton);
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
                JOptionPane.showMessageDialog(this, "Порт должен быть числом", "Проверка", JOptionPane.WARNING_MESSAGE);
            }
        });
        foldButton.addActionListener(event -> controller.sendAction("FOLD", 0));
        checkButton.addActionListener(event -> controller.sendAction("CHECK", 0));
        callButton.addActionListener(event -> controller.sendAction("CALL", 0));
        raiseButton.addActionListener(event -> controller.sendAction("RAISE", (Integer) raiseSpinner.getValue()));
        allInButton.addActionListener(event -> controller.sendAction("ALL_IN", 0));
        myCombinationButton.addActionListener(event -> showCombinationDialog());
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

    private void showCombinationDialog() {
        JFrame dialog = new JFrame("Моя комбинация");
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(new Color(24, 94, 52));

        String effectiveCombo = currentCombination == null || currentCombination.isBlank()
                ? "Недостаточно карт"
                : currentCombination;
        JLabel currentLabel = new JLabel("Текущая комбинация: " + effectiveCombo);
        currentLabel.setForeground(Color.WHITE);
        currentLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        dialog.add(currentLabel, BorderLayout.NORTH);

        JPanel listPanel = new JPanel(new GridLayout(COMBINATION_ORDER.length, 1, 4, 4));
        listPanel.setOpaque(false);
        for (String combination : COMBINATION_ORDER) {
            JLabel label = new JLabel(combination);
            label.setOpaque(true);
            boolean selected = combination.equals(currentCombination);
            label.setBackground(selected ? new Color(255, 215, 0) : new Color(241, 235, 221));
            label.setForeground(selected ? new Color(76, 51, 26) : Color.BLACK);
            label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            listPanel.add(label);
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        wrapper.add(listPanel, BorderLayout.CENTER);
        dialog.add(wrapper, BorderLayout.CENTER);

        dialog.setSize(320, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private String translateStage(String stage) {
        return switch (stage) {
            case "WAITING" -> "ожидание";
            case "PRE_FLOP" -> "префлоп";
            case "FLOP" -> "флоп";
            case "TURN" -> "тёрн";
            case "RIVER" -> "ривер";
            case "SHOWDOWN" -> "вскрытие";
            default -> stage;
        };
    }
}
