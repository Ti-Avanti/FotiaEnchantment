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
    void materialLimitAcceptsCommonKeyFormats() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("default-max-enchantments", 8);
        config.set("materials.netherite-helmet", 2);
        config.set("materials.minecraft:diamond_chestplate", 4);

        assertEquals(2, EnchantmentLimitPolicy.resolveLimit(config, Material.NETHERITE_HELMET, 10));
        assertEquals(4, EnchantmentLimitPolicy.resolveLimit(config, Material.DIAMOND_CHESTPLATE, 10));
    }

    @Test
    void groupLimitAcceptsCommonKeyFormats() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("default-max-enchantments", 8);
        config.set("item-groups.fishing_rods", 1);
        config.set("item-groups.crossBows", 2);

        assertEquals(1, EnchantmentLimitPolicy.resolveLimit(config, Material.FISHING_ROD, 10));
        assertEquals(2, EnchantmentLimitPolicy.resolveLimit(config, Material.CROSSBOW, 10));
    }

    @Test
    void negativeLimitMeansUnlimitedAndZeroBlocksNewEnchantments() {
        assertTrue(EnchantmentLimitPolicy.canAddNewEnchantment(128, -1));
        assertFalse(EnchantmentLimitPolicy.canAddNewEnchantment(0, 0));
        assertFalse(EnchantmentLimitPolicy.canAddNewEnchantment(2, 2));
        assertTrue(EnchantmentLimitPolicy.canAddNewEnchantment(1, 2));
    }

    @Test
    void knownItemGroupsCanDisplayEnchantSlotsBeforeAnyEnchantIsApplied() {
        assertTrue(EnchantmentLimitPolicy.hasKnownItemGroup(Material.DIAMOND_SWORD));
        assertTrue(EnchantmentLimitPolicy.hasKnownItemGroup(Material.BOW));
        assertFalse(EnchantmentLimitPolicy.hasKnownItemGroup(Material.DIRT));
    }
}
