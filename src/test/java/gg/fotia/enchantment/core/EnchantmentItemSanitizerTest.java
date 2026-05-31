package gg.fotia.enchantment.core;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnchantmentItemSanitizerTest {

    @Test
    void keepsOnlyEnabledApplicableEnchantmentsAndPreservesConfiguredLevel() {
        EnchantmentData valid = enchantment("valid", true, 3, Material.DIAMOND_SWORD);
        EnchantmentData disabled = enchantment("disabled", false, 3, Material.DIAMOND_SWORD);
        EnchantmentData wrongItem = enchantment("wrong_item", true, 3, Material.DIAMOND_PICKAXE);

        EnchantmentItemSanitizer.ValidityRules rules = EnchantmentItemSanitizer.ValidityRules.from(
                Map.of(
                        "valid", valid,
                        "disabled", disabled,
                        "wrong_item", wrongItem
                ).values()
        );

        Map<String, Integer> result = EnchantmentItemSanitizer.validEnchantments(
                Map.of(
                        "valid", 9,
                        "disabled", 1,
                        "wrong_item", 1,
                        "deleted", 1
                ),
                Material.DIAMOND_SWORD,
                rules
        );

        assertEquals(Map.of("valid", 9), result);
    }

    @Test
    void keepsEnabledEnchantmentsOnEnchantedBooksRegardlessOfTargetMaterial() {
        EnchantmentData pickaxeOnly = enchantment("pickaxe_only", true, 1, Material.DIAMOND_PICKAXE);

        EnchantmentItemSanitizer.ValidityRules rules = EnchantmentItemSanitizer.ValidityRules.from(
                java.util.List.of(pickaxeOnly)
        );

        Map<String, Integer> result = EnchantmentItemSanitizer.validEnchantments(
                Map.of("pickaxe_only", 1),
                Material.ENCHANTED_BOOK,
                rules
        );

        assertEquals(Map.of("pickaxe_only", 1), result);
    }

    private static EnchantmentData enchantment(String id, boolean enabled, int maxLevel, Material material) {
        EnchantmentData data = new EnchantmentData();
        data.setId(id);
        data.setEnabled(enabled);
        data.setMaxLevel(maxLevel);
        data.setApplicableItems(java.util.List.of(material));
        return data;
    }
}
