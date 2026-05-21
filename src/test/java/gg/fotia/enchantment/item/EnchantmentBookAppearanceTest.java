package gg.fotia.enchantment.item;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnchantmentBookAppearanceTest {

    @Test
    void rarityOverridesDefaultBookAppearance() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("default.custom-model-data", 21000);
        config.set("default.item-model", "fotiaenchantment:book_default");
        config.set("default.tooltip-style", "minecraft:default_book");
        config.set("radiant.modeldata", 21012);
        config.set("radiant.itemmodel", "fotiaenchantment:book_radiant");

        EnchantmentBookAppearance appearance = EnchantmentBookAppearance.from(config, "radiant");

        assertEquals(Integer.valueOf(21012), appearance.customModelData());
        assertEquals("fotiaenchantment:book_radiant", appearance.itemModel());
        assertEquals("minecraft:default_book", appearance.tooltipStyle());
    }

    @Test
    void unknownRarityUsesDefaultBookAppearance() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("default.custom-model-data", 21000);
        config.set("default.item-model", "fotiaenchantment:book_default");
        config.set("default.tooltip-style", "minecraft:default_book");

        EnchantmentBookAppearance appearance = EnchantmentBookAppearance.from(config, "unknown");

        assertEquals(Integer.valueOf(21000), appearance.customModelData());
        assertEquals("fotiaenchantment:book_default", appearance.itemModel());
        assertEquals("minecraft:default_book", appearance.tooltipStyle());
    }
}
