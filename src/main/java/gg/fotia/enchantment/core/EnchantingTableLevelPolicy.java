package gg.fotia.enchantment.core;

import java.util.List;
import java.util.Map;

public final class EnchantingTableLevelPolicy {

    private EnchantingTableLevelPolicy() {
    }

    public static List<LevelTier> defaultTiers() {
        return List.of(
                new LevelTier(10, Map.of(
                        1, 100,
                        2, 10
                )),
                new LevelTier(20, Map.of(
                        1, 70,
                        2, 45,
                        3, 15,
                        4, 3
                )),
                new LevelTier(30, Map.of(
                        1, 35,
                        2, 45,
                        3, 30,
                        4, 14,
                        5, 5
                ))
        );
    }

    public static int rollLevel(int maxLevel,
                                int expLevelCost,
                                int seed,
                                String enchantmentKey,
                                int offerSlot,
                                List<LevelTier> tiers) {
        int cap = Math.max(1, maxLevel);
        LevelTier tier = selectTier(expLevelCost, tiers == null || tiers.isEmpty() ? defaultTiers() : tiers);
        if (tier == null || tier.weights().isEmpty()) {
            return legacyScaledLevel(cap, expLevelCost);
        }

        int totalWeight = 0;
        for (int level = 1; level <= cap; level++) {
            totalWeight += Math.max(0, tier.weights().getOrDefault(level, 0));
        }
        if (totalWeight <= 0) {
            return cap;
        }

        int roll = stableLevelRoll(totalWeight, seed, expLevelCost, enchantmentKey, offerSlot, cap);
        int cursor = 0;
        for (int level = 1; level <= cap; level++) {
            cursor += Math.max(0, tier.weights().getOrDefault(level, 0));
            if (roll < cursor) {
                return level;
            }
        }
        return cap;
    }

    public static int legacyScaledLevel(int maxLevel, int expLevelCost) {
        int cap = Math.max(1, maxLevel);
        return Math.max(1, Math.min(cap, (int) Math.ceil(Math.max(1, expLevelCost) * cap / 30.0)));
    }

    private static LevelTier selectTier(int expLevelCost, List<LevelTier> tiers) {
        LevelTier selected = null;
        LevelTier fallback = null;
        int cost = Math.max(1, expLevelCost);
        for (LevelTier tier : tiers) {
            if (tier == null || tier.maxCost() <= 0) {
                continue;
            }
            if (fallback == null || tier.maxCost() > fallback.maxCost()) {
                fallback = tier;
            }
            if (cost <= tier.maxCost() && (selected == null || tier.maxCost() < selected.maxCost())) {
                selected = tier;
            }
        }
        return selected == null ? fallback : selected;
    }

    private static int stableLevelRoll(int totalWeight,
                                       int seed,
                                       int expLevelCost,
                                       String enchantmentKey,
                                       int offerSlot,
                                       int maxLevel) {
        long hash = 0xcbf29ce484222325L;
        hash = mix(hash, seed);
        hash = mix(hash, expLevelCost);
        hash = mix(hash, enchantmentKey == null ? "" : enchantmentKey);
        hash = mix(hash, offerSlot);
        hash = mix(hash, maxLevel);
        return (int) Math.floorMod(finalMix(hash), (long) totalWeight);
    }

    private static long mix(long hash, int value) {
        return finalMix(hash ^ value);
    }

    private static long mix(long hash, String value) {
        long result = hash;
        for (int i = 0; i < value.length(); i++) {
            result = mix(result, value.charAt(i));
        }
        return result;
    }

    private static long finalMix(long value) {
        long result = value;
        result ^= result >>> 33;
        result *= 0xff51afd7ed558ccdL;
        result ^= result >>> 33;
        result *= 0xc4ceb9fe1a85ec53L;
        result ^= result >>> 33;
        return result;
    }

    public record LevelTier(int maxCost, Map<Integer, Integer> weights) {
        public LevelTier {
            weights = weights == null ? Map.of() : Map.copyOf(weights);
        }
    }
}
