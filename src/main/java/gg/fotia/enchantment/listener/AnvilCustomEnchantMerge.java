package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.core.EnchantmentData;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

final class AnvilCustomEnchantMerge {

    private AnvilCustomEnchantMerge() {
    }

    static Result merge(Map<String, Integer> existing,
                        Map<String, Integer> incoming,
                        Function<String, EnchantmentData> dataResolver,
                        Predicate<EnchantmentData> applicable,
                        boolean targetIsEnchantedBook,
                        int currentEnchantCount,
                        int maxEnchantments) {
        Map<String, Integer> merged = normalizedCopy(existing);
        if (incoming == null || incoming.isEmpty() || dataResolver == null) {
            return new Result(Map.copyOf(merged), false);
        }

        int currentCount = Math.max(merged.size(), currentEnchantCount);
        int limit = Math.max(0, maxEnchantments);
        boolean modified = false;

        for (Map.Entry<String, Integer> entry : incoming.entrySet()) {
            String id = normalize(entry.getKey());
            int incomingLevel = entry.getValue() == null ? 0 : entry.getValue();
            if (id.isEmpty() || incomingLevel <= 0) {
                continue;
            }

            EnchantmentData data = dataResolver.apply(id);
            if (data == null || !data.isEnabled() || !data.getObtain().isAnvil()) {
                continue;
            }
            if (!targetIsEnchantedBook && applicable != null && !applicable.test(data)) {
                continue;
            }
            if (hasConflict(id, data, merged, dataResolver)) {
                continue;
            }

            int existingLevel = merged.getOrDefault(id, 0);
            int maxLevel = Math.max(1, data.getMaxLevel());
            int newLevel = mergedLevel(existingLevel, incomingLevel, maxLevel);
            if (existingLevel == 0) {
                if (currentCount >= limit) {
                    continue;
                }
                currentCount++;
            }
            if (newLevel > existingLevel) {
                merged.put(id, newLevel);
                modified = true;
            }
        }

        return new Result(Map.copyOf(merged), modified);
    }

    private static int mergedLevel(int existingLevel, int incomingLevel, int maxLevel) {
        if (existingLevel <= 0) {
            return Math.min(incomingLevel, maxLevel);
        }
        if (existingLevel == incomingLevel) {
            return Math.min(existingLevel + 1, maxLevel);
        }
        return Math.min(Math.max(existingLevel, incomingLevel), maxLevel);
    }

    private static boolean hasConflict(String id,
                                       EnchantmentData data,
                                       Map<String, Integer> existing,
                                       Function<String, EnchantmentData> dataResolver) {
        for (String existingId : existing.keySet()) {
            if (id.equals(existingId)) {
                continue;
            }
            if (data.getConflicts().contains(existingId)) {
                return true;
            }
            EnchantmentData existingData = dataResolver.apply(existingId);
            if (existingData != null && existingData.getConflicts().contains(id)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Integer> normalizedCopy(Map<String, Integer> source) {
        Map<String, Integer> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            String id = normalize(entry.getKey());
            int level = entry.getValue() == null ? 0 : entry.getValue();
            if (!id.isEmpty() && level > 0) {
                copy.merge(id, level, Math::max);
            }
        }
        return copy;
    }

    private static String normalize(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }

    record Result(Map<String, Integer> enchantments, boolean modified) {
    }
}
