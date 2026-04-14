package poker.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HandEvaluator {
    private HandEvaluator() {
    }

    public static HandRank evaluateBest(List<Card> cards) {
        if (cards.size() < 5 || cards.size() > 7) {
            throw new IllegalArgumentException("Expected 5 to 7 cards");
        }
        if (cards.size() == 5) {
            return evaluateFive(cards);
        }
        HandRank best = null;
        for (int a = 0; a < cards.size() - 4; a++) {
            for (int b = a + 1; b < cards.size() - 3; b++) {
                for (int c = b + 1; c < cards.size() - 2; c++) {
                    for (int d = c + 1; d < cards.size() - 1; d++) {
                        for (int e = d + 1; e < cards.size(); e++) {
                            HandRank current = evaluateFive(List.of(
                                    cards.get(a), cards.get(b), cards.get(c), cards.get(d), cards.get(e)));
                            if (best == null || current.compareTo(best) > 0) {
                                best = current;
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    public static HandRank evaluateSeven(List<Card> cards) {
        if (cards.size() != 7) {
            throw new IllegalArgumentException("Expected 7 cards");
        }
        return evaluateBest(cards);
    }

    private static HandRank evaluateFive(List<Card> cards) {
        List<Integer> ranks = cards.stream()
                .map(card -> card.rank().value())
                .sorted(Comparator.reverseOrder())
                .toList();
        boolean flush = cards.stream().map(Card::suit).distinct().count() == 1;
        int straightHigh = straightHigh(ranks);

        Map<Integer, Integer> counts = new HashMap<>();
        for (int rank : ranks) {
            counts.merge(rank, 1, Integer::sum);
        }

        List<Map.Entry<Integer, Integer>> byCount = new ArrayList<>(counts.entrySet());
        byCount.sort((left, right) -> {
            int countCmp = Integer.compare(right.getValue(), left.getValue());
            return countCmp != 0 ? countCmp : Integer.compare(right.getKey(), left.getKey());
        });

        if (flush && straightHigh == 14) {
            return new HandRank(10, List.of(14), "Royal Flush");
        }
        if (flush && straightHigh > 0) {
            return new HandRank(9, List.of(straightHigh), "Straight Flush");
        }
        if (byCount.get(0).getValue() == 4) {
            int four = byCount.get(0).getKey();
            int kicker = byCount.get(1).getKey();
            return new HandRank(8, List.of(four, kicker), "Four of a Kind");
        }
        if (byCount.get(0).getValue() == 3 && byCount.get(1).getValue() == 2) {
            return new HandRank(7, List.of(byCount.get(0).getKey(), byCount.get(1).getKey()), "Full House");
        }
        if (flush) {
            return new HandRank(6, ranks, "Flush");
        }
        if (straightHigh > 0) {
            return new HandRank(5, List.of(straightHigh), "Straight");
        }
        if (byCount.get(0).getValue() == 3) {
            List<Integer> kickers = new ArrayList<>();
            kickers.add(byCount.get(0).getKey());
            byCount.stream()
                    .filter(entry -> entry.getValue() == 1)
                    .map(Map.Entry::getKey)
                    .sorted(Comparator.reverseOrder())
                    .forEach(kickers::add);
            return new HandRank(4, kickers, "Three of a Kind");
        }
        if (byCount.get(0).getValue() == 2 && byCount.get(1).getValue() == 2) {
            int highPair = Math.max(byCount.get(0).getKey(), byCount.get(1).getKey());
            int lowPair = Math.min(byCount.get(0).getKey(), byCount.get(1).getKey());
            int kicker = byCount.stream().filter(entry -> entry.getValue() == 1).findFirst().orElseThrow().getKey();
            return new HandRank(3, List.of(highPair, lowPair, kicker), "Two Pair");
        }
        if (byCount.get(0).getValue() == 2) {
            List<Integer> kickers = new ArrayList<>();
            kickers.add(byCount.get(0).getKey());
            byCount.stream()
                    .filter(entry -> entry.getValue() == 1)
                    .map(Map.Entry::getKey)
                    .sorted(Comparator.reverseOrder())
                    .forEach(kickers::add);
            return new HandRank(2, kickers, "One Pair");
        }
        return new HandRank(1, ranks, "High Card");
    }

    private static int straightHigh(List<Integer> sortedDescending) {
        List<Integer> unique = new ArrayList<>();
        for (int rank : sortedDescending) {
            if (!unique.contains(rank)) {
                unique.add(rank);
            }
        }
        if (unique.contains(14)) {
            unique.add(1);
        }
        Collections.sort(unique);
        int streak = 1;
        int best = 0;
        for (int i = 1; i < unique.size(); i++) {
            if (unique.get(i) == unique.get(i - 1) + 1) {
                streak++;
                if (streak >= 5) {
                    best = unique.get(i);
                }
            } else {
                streak = 1;
            }
        }
        return best == 1 ? 5 : best;
    }
}
