package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.core.EnchantmentData;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnvilCustomEnchantMergeTest {

    @Test
    void sameLevelBooksUpgradeEvenWhenEnchantDoesNotApplyToBooks() {
        EnchantmentData data = enchant("blaze_resist", 3);

        AnvilCustomEnchantMerge.Result result = AnvilCustomEnchantMerge.merge(
                Map.of("blaze_resist", 1),
                Map.of("blaze_resist", 1),
                id -> data,
                ignored -> false,
                true,
                1,
                8
        );

        assertTrue(result.modified());
        assertEquals(2, result.enchantments().get("blaze_resist"));
    }

    @Test
    void itemTargetsStillRequireApplicability() {
        EnchantmentData data = enchant("blaze_resist", 3);

        AnvilCustomEnchantMerge.Result result = AnvilCustomEnchantMerge.merge(
                Map.of(),
                Map.of("blaze_resist", 1),
                id -> data,
                ignored -> false,
                false,
                0,
                8
        );

        assertFalse(result.modified());
        assertEquals(Map.of(), result.enchantments());
    }

    @Test
    void equalLevelsAreCappedAtMaxLevel() {
        EnchantmentData data = enchant("blaze_resist", 2);

        AnvilCustomEnchantMerge.Result result = AnvilCustomEnchantMerge.merge(
                Map.of("blaze_resist", 2),
                Map.of("blaze_resist", 2),
                id -> data,
                ignored -> true,
                true,
                1,
                8
        );

        assertFalse(result.modified());
        assertEquals(2, result.enchantments().get("blaze_resist"));
    }

    @Test
    void newEnchantRespectsExistingEnchantLimitCount() {
        EnchantmentData data = enchant("blaze_resist", 3);

        AnvilCustomEnchantMerge.Result result = AnvilCustomEnchantMerge.merge(
                Map.of(),
                Map.of("blaze_resist", 1),
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
    void unlimitedEnchantLimitAllowsNewEnchantments() {
        EnchantmentData data = enchant("blaze_resist", 3);

        AnvilCustomEnchantMerge.Result result = AnvilCustomEnchantMerge.merge(
                Map.of(),
                Map.of("blaze_resist", 1),
                id -> data,
                ignored -> true,
                false,
                128,
                -1
        );

        assertTrue(result.modified());
        assertEquals(1, result.enchantments().get("blaze_resist"));
    }

    @Test
    void overLimitExistingEnchantCanStillUpgradeSameEntry() {
        EnchantmentData data = enchant("blaze_resist", 3);

        AnvilCustomEnchantMerge.Result result = AnvilCustomEnchantMerge.merge(
                Map.of("blaze_resist", 1),
                Map.of("blaze_resist", 1),
                id -> data,
                ignored -> true,
                false,
                9,
                8
        );

        assertTrue(result.modified());
        assertEquals(2, result.enchantments().get("blaze_resist"));
    }

    @Test
    void reverseConflictDefinitionBlocksIncomingEnchant() {
        EnchantmentData incoming = enchant("blaze_resist", 3);
        EnchantmentData existing = enchant("frost_shield", 3);
        existing.setConflicts(java.util.List.of("blaze_resist"));

        AnvilCustomEnchantMerge.Result result = AnvilCustomEnchantMerge.merge(
                Map.of("frost_shield", 1),
                Map.of("blaze_resist", 1),
                id -> "frost_shield".equals(id) ? existing : incoming,
                ignored -> true,
                false,
                1,
                8
        );

        assertFalse(result.modified());
        assertEquals(Map.of("frost_shield", 1), result.enchantments());
    }

    private static EnchantmentData enchant(String id, int maxLevel) {
        EnchantmentData data = new EnchantmentData();
        data.setId(id);
        data.setEnabled(true);
        data.setMaxLevel(maxLevel);
        data.getObtain().setAnvil(true);
        return data;
    }
}
