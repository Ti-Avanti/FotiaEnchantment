package gg.fotia.enchantment.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantingTableLevelPolicyTest {

    @Test
    void highCostRollsCanStillProduceNonMaximumLevels() {
        List<EnchantingTableLevelPolicy.LevelTier> tiers = List.of(
                new EnchantingTableLevelPolicy.LevelTier(30, Map.of(
                        1, 35,
                        2, 45,
                        3, 30,
                        4, 14,
                        5, 5
                ))
        );

        Set<Integer> levels = IntStream.range(0, 64)
                .map(seed -> EnchantingTableLevelPolicy.rollLevel(5, 30, seed, "fotiaenchantment:probe", 2, tiers))
                .boxed()
                .collect(Collectors.toSet());

        assertTrue(levels.contains(5), "High-cost rolls should still allow maximum level");
        assertTrue(levels.stream().anyMatch(level -> level < 5),
                "High-cost rolls must not force every Fotia enchantment to maximum level");
    }

    @Test
    void configuredWeightsAboveTheEnchantMaxLevelAreIgnored() {
        List<EnchantingTableLevelPolicy.LevelTier> tiers = List.of(
                new EnchantingTableLevelPolicy.LevelTier(30, Map.of(5, 100))
        );

        assertEquals(3, EnchantingTableLevelPolicy.rollLevel(3, 30, 1, "fotiaenchantment:probe", 0, tiers));
    }
}
