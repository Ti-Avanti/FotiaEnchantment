package gg.fotia.enchantment.core;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MobDropEnchantmentLimitPolicy {

    private MobDropEnchantmentLimitPolicy() {
    }

    public static Set<String> keysToRemove(Map<String, Integer> enchantments, int max) {
        if (max < 0 || enchantments == null || enchantments.isEmpty()) {
            return Set.of();
        }

        List<Map.Entry<String, Integer>> ranked = enchantments.entrySet().stream()
                .filter(entry -> entry.getKey() != null
                        && !entry.getKey().isBlank()
                        && entry.getValue() != null
                        && entry.getValue() > 0)
                .sorted(Comparator
                        .<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .toList();
        if (ranked.size() <= max) {
            return Set.of();
        }

        Set<String> removed = new LinkedHashSet<>();
        for (int index = Math.max(0, max); index < ranked.size(); index++) {
            removed.add(ranked.get(index).getKey());
        }
        return Set.copyOf(removed);
    }
}
