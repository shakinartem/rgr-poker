package poker.client;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Component;
import java.util.List;

public final class SeatPanel extends JPanel {
    private final JLabel nameLabel = new JLabel("Пусто", SwingConstants.CENTER);
    private final JLabel stackLabel = new JLabel("стек=0", SwingConstants.CENTER);
    private final JLabel stateLabel = new JLabel("-", SwingConstants.CENTER);
    private final JLabel firstCardLabel = createCardLabel();
    private final JLabel secondCardLabel = createCardLabel();

    public SeatPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createLineBorder(new Color(76, 51, 26), 2));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        stackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        stateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel cardsPanel = new JPanel();
        cardsPanel.setOpaque(false);
        cardsPanel.add(firstCardLabel);
        cardsPanel.add(secondCardLabel);
        cardsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(nameLabel);
        add(stackLabel);
        add(stateLabel);
        add(cardsPanel);
    }

    public void render(String name, int stack, int roundBet, boolean folded, boolean allIn, boolean connected,
                       boolean waiting, List<String> cards) {
        nameLabel.setText(name);
        stackLabel.setText("стек=" + stack + " ставка=" + roundBet);
        StringBuilder state = new StringBuilder();
        if (waiting) {
            state.append("ОЖИДАЕТ ");
        }
        if (folded) {
            state.append("ПАС ");
        }
        if (allIn) {
            state.append("ВА-БАНК ");
        }
        if (!connected) {
            state.append("НЕ В СЕТИ ");
        }
        stateLabel.setText(state.length() == 0 ? "АКТИВЕН" : state.toString().trim());
        firstCardLabel.setIcon(resolve(cards, 0));
        secondCardLabel.setIcon(resolve(cards, 1));
        boolean highlight = stateLabel.getText().contains("АКТИВЕН") && connected;
        setBorder(BorderFactory.createLineBorder(highlight ? new Color(214, 183, 91) : new Color(76, 51, 26), 2));
    }

    public void clear() {
        render("Пусто", 0, 0, false, false, false, false, List.of());
    }

    private JLabel createCardLabel() {
        JLabel label = new JLabel(CardImageCache.load("BACK"));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    private ImageIcon resolve(List<String> cards, int index) {
        return index < cards.size() ? CardImageCache.load(cards.get(index)) : CardImageCache.load("BACK");
    }
}
