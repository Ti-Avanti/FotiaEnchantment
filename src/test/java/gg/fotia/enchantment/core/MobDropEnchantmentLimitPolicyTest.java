package gg.fotia.enchantment.core;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobDropEnchantmentLimitPolicyTest {

    @Test
    void trimsLowestLevelEnchantmentsFirst() {
        Map<String, Integer> enchantments = new LinkedHashMap<>();
        enchantments.put("minecraft:sharpness", 4);
        enchantments.put("fotiaenchantment:lightning", 1);
        enchantments.put("minecraft:unbreaking", 3);
        enchantments.put("minecraft:fire_aspect", 2);

        Set<String> removed = MobDropEnchantmentLimitPolicy.keysToRemove(enchantments, 3);

        assertEquals(Set.of("fotiaenchantment:lightning"), removed);
    }

    @Test
    void usesNamespacedKeyAsStableTieBreaker() {
        Map<String, Integer> enchantments = new LinkedHashMap<>();
        enchantments.put("minecraft:unbreaking", 3);
        enchantments.put("fotiaenchantment:lightning", 3);
        enchantments.put("minecraft:sharpness", 3);

        Set<String> removed = MobDropEnchantmentLimitPolicy.keysToRemove(enchantments, 2);

        assertEquals(Set.of("minecraft:unbreaking"), removed);
    }

    @Test
    void unlimitedItemsAreNeverTrimmed() {
        assertTrue(MobDropEnchantmentLimitPolicy.keysToRemove(
                Map.of("minecraft:sharpness", 5, "minecraft:unbreaking", 3), -1).isEmpty());
    }

    @Test
    void zeroLimitRemovesEveryEnchantment() {
        assertEquals(
                Set.of("minecraft:sharpness", "fotiaenchantment:lightning"),
                MobDropEnchantmentLimitPolicy.keysToRemove(
                        Map.of("minecraft:sharpness", 5, "fotiaenchantment:lightning", 2), 0));
    }

    @Test
    void overflowActionFallsBackToTrim() {
        assertEquals(MobDropOverflowAction.TRIM, MobDropOverflowAction.parse(null));
        assertEquals(MobDropOverflowAction.TRIM, MobDropOverflowAction.parse("unknown"));
        assertEquals(MobDropOverflowAction.REMOVE_DROP, MobDropOverflowAction.parse("remove_drop"));
    }
}
