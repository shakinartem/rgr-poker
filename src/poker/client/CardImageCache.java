package poker.client;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CardImageCache {
    private static final String ASSET_DIR = "assets/cards";
    private static final int CARD_WIDTH = 84;
    private static final int CARD_HEIGHT = 116;
    private static final Map<String, ImageIcon> CACHE = new ConcurrentHashMap<>();

    private CardImageCache() {
    }

    public static ImageIcon load(String code) {
        return CACHE.computeIfAbsent(code == null || code.isBlank() ? "BACK" : code, CardImageCache::readIcon);
    }

    private static ImageIcon readIcon(String code) {
        File file = new File(ASSET_DIR, code + ".png");
        if (!file.isFile()) {
            file = new File(ASSET_DIR, "BACK.png");
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                return new ImageIcon();
            }
            Image scaled = image.getScaledInstance(CARD_WIDTH, CARD_HEIGHT, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (IOException exception) {
            return new ImageIcon();
        }
    }
}
