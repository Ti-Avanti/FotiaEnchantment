package gg.fotia.enchantment.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentConflictPolicyTest {

    @Test
    void customConflictMatchesFotiaNamespacedExistingIds() {
        EnchantmentData candidate = enchant("frost_shield", "flame_guard");

        assertTrue(EnchantmentConflictPolicy.hasCustomConflict(
                "frost_shield",
                candidate,
                Set.of("fotiaenchantment:flame_guard"),
                id -> null
        ));
    }

    @Test
    void customConflictChecksReverseDefinitions() {
        EnchantmentData candidate = enchant("frost_shield");
        EnchantmentData existing = enchant("flame_guard", "frost_shield");
        Map<String, EnchantmentData> data = Map.of("flame_guard", existing);

        assertTrue(EnchantmentConflictPolicy.hasCustomConflict(
                "frost_shield",
                candidate,
                Set.of("flame_guard"),
                data::get
        ));
    }

    @Test
    void customConflictDoesNotTreatMinecraftKeyAsUnqualifiedCustomId() {
        EnchantmentData candidate = enchant("sharpness_like", "sharpness");

        assertFalse(EnchantmentConflictPolicy.hasCustomConflict(
                "sharpness_like",
                candidate,
                Set.of("minecraft:sharpness"),
                id -> null
        ));
    }

    private static EnchantmentData enchant(String id, String... conflicts) {
        EnchantmentData data = new EnchantmentData();
        data.setId(id);
        data.setConflicts(List.of(conflicts));
        return data;
    }
}
