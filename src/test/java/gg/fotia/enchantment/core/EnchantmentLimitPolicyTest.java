package gg.fotia.enchantment.core;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentLimitPolicyTest {

    @Test
    void materialLimitOverridesGroupAndDefaultLimit() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("default-max-enchantments", 8);
        config.set("item-groups.chestplates", 6);
        config.set("materials.DIAMOND_CHESTPLATE", 4);

        int limit = EnchantmentLimitPolicy.resolveLimit(config, Material.DIAMOND_CHESTPLATE, 10);

        assertEquals(4, limit);
    }

    @Test
    void groupLimitAppliesToAllAxesWhenMaterialHasNoOverride() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("default-max-enchantments", 8);
        config.set("item-groups.axes", 3);

        assertEquals(3, EnchantmentLimitPolicy.resolveLimit(config, Material.NETHERITE_AXE, 10));
        assertEquals(8, EnchantmentLimitPolicy.resolveLimit(config, Material.DIAMOND_SWORD, 10));
    }

    @Test
    void negativeLimitMeansUnlimitedAndZeroBlocksNewEnchantments() {
        assertTrue(EnchantmentLimitPolicy.canAddNewEnchantment(128, -1));
        assertFalse(EnchantmentLimitPolicy.canAddNewEnchantment(0, 0));
        assertFalse(EnchantmentLimitPolicy.canAddNewEnchantment(2, 2));
        assertTrue(EnchantmentLimitPolicy.canAddNewEnchantment(1, 2));
    }
}
