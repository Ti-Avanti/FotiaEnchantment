package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.core.EnchantmentData;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnvilBreakthroughServiceTest {

    @Test
    void breakthroughMergeUsesAnvilRulesWithoutAllowingConflicts() {
        EnchantmentData incoming = enchant("flame_edge", 3, Material.DIAMOND_SWORD);
        EnchantmentData existing = enchant("frost_edge", 3, Material.DIAMOND_SWORD);
        existing.setConflicts(List.of("flame_edge"));

        AnvilBreakthroughService.Result result = AnvilBreakthroughService.mergeCustomEnchantments(
                Map.of("frost_edge", 1),
                Map.of("flame_edge", 1),
                id -> "frost_edge".equals(id) ? existing : incoming,
                data -> true,
                false,
                1,
                8
        );

        assertFalse(result.modified());
        assertEquals(Map.of("frost_edge", 1), result.enchantments());
    }

    @Test
    void breakthroughMergeCanUpgradeExistingEntryEvenWhenItemIsAlreadyOverLimit() {
        EnchantmentData data = enchant("flame_edge", 3, Material.DIAMOND_SWORD);

        AnvilBreakthroughService.Result result = AnvilBreakthroughService.mergeCustomEnchantments(
                Map.of("flame_edge", 1),
                Map.of("flame_edge", 1),
                id -> data,
                ignored -> true,
                false,
                9,
                8
        );

        assertTrue(result.modified());
        assertEquals(2, result.enchantments().get("flame_edge"));
    }

    @Test
    void breakthroughMergeDoesNotBypassNewEnchantLimit() {
        EnchantmentData data = enchant("flame_edge", 3, Material.DIAMOND_SWORD);

        AnvilBreakthroughService.Result result = AnvilBreakthroughService.mergeCustomEnchantments(
                Map.of(),
                Map.of("flame_edge", 1),
                id -> data,
                ignored -> true,
                false,
                8,
                8
        );

        assertFalse(result.modified());
        assertEquals(Map.of(), result.enchantments());
    }

    @Test
    void breakthroughMergeSupportsVanillaStoredEnchantments() {
        String sharpness = "minecraft:sharpness";

        AnvilBreakthroughService.VanillaResult<String> result = AnvilBreakthroughService.mergeVanillaEnchantments(
                Map.of(),
                Map.of(sharpness, 10),
                enchantment -> true,
                (enchantment, existing) -> false,
                enchantment -> 10,
                0,
                8
        );

        assertTrue(result.modified());
        assertEquals(10, result.enchantments().get(sharpness));
    }

    @Test
    void breakthroughMergeDoesNotBypassVanillaNewEnchantLimit() {
        String sharpness = "minecraft:sharpness";

        AnvilBreakthroughService.VanillaResult<String> result = AnvilBreakthroughService.mergeVanillaEnchantments(
                Map.of(),
                Map.of(sharpness, 10),
                enchantment -> true,
                (enchantment, existing) -> false,
                enchantment -> 10,
                8,
                8
        );

        assertFalse(result.modified());
        assertEquals(Map.of(), result.enchantments());
    }

    @Test
    void breakthroughMergeCanUpgradeExistingVanillaEnchantmentOverLimit() {
        String sharpness = "minecraft:sharpness";
        Map<String, Integer> existing = new HashMap<>();
        existing.put(sharpness, 4);

        AnvilBreakthroughService.VanillaResult<String> result = AnvilBreakthroughService.mergeVanillaEnchantments(
                existing,
                Map.of(sharpness, 4),
                enchantment -> true,
                (enchantment, current) -> false,
                enchantment -> 10,
                9,
                8
        );

        assertTrue(result.modified());
        assertEquals(5, result.enchantments().get(sharpness));
    }

    private static EnchantmentData enchant(String id, int maxLevel, Material material) {
        EnchantmentData data = new EnchantmentData();
        data.setId(id);
        data.setEnabled(true);
        data.setMaxLevel(maxLevel);
        data.setApplicableItems(List.of(material));
        data.getObtain().setAnvil(true);
        return data;
    }
}
