package poker.common;

import java.util.ArrayList;
import java.util.List;

public final class HandRank implements Comparable<HandRank> {
    private final int category;
    private final List<Integer> kickers;
    private final String description;

    public HandRank(int category, List<Integer> kickers, String description) {
        this.category = category;
        this.kickers = new ArrayList<>(kickers);
        this.description = description;
    }

    public int category() {
        return category;
    }

    public List<Integer> kickers() {
        return List.copyOf(kickers);
    }

    public String description() {
        return description;
    }

    @Override
    public int compareTo(HandRank other) {
        int categoryCompare = Integer.compare(category, other.category);
        if (categoryCompare != 0) {
            return categoryCompare;
        }
        int size = Math.min(kickers.size(), other.kickers.size());
        for (int i = 0; i < size; i++) {
            int cmp = Integer.compare(kickers.get(i), other.kickers.get(i));
            if (cmp != 0) {
                return cmp;
            }
        }
        return Integer.compare(kickers.size(), other.kickers.size());
    }

    @Override
    public String toString() {
        return description;
    }
}
