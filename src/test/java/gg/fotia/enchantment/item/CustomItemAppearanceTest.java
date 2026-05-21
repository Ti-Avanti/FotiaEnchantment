package gg.fotia.enchantment.item;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CustomItemAppearanceTest {

    @Test
    void readsModelDataItemModelAndTooltipStyleAliases() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("modeldata", 10015);
        config.set("itemmodel", "fotiaenchantment:random_codex");
        config.set("tooltip-style", "fotiaenchantment:random_codex_tooltip");

        CustomItemAppearance appearance = CustomItemAppearance.from(config);

        assertEquals(Integer.valueOf(10015), appearance.customModelData());
        assertEquals("fotiaenchantment:random_codex", appearance.itemModel());
        assertEquals("fotiaenchantment:random_codex_tooltip", appearance.tooltipStyle());
    }

    @Test
    void readsHyphenatedKeys() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("custom-model-data", 10012);
        config.set("item-model", "fotiaenchantment:radiant_codex");
        config.set("tooltip-style", "fotiaenchantment:radiant_tooltip");

        CustomItemAppearance appearance = CustomItemAppearance.from(config);

        assertEquals(Integer.valueOf(10012), appearance.customModelData());
        assertEquals("fotiaenchantment:radiant_codex", appearance.itemModel());
        assertEquals("fotiaenchantment:radiant_tooltip", appearance.tooltipStyle());
    }

    @Test
    void blankValuesAreIgnored() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("item-model", "");
        config.set("tooltip-style", "   ");

        CustomItemAppearance appearance = CustomItemAppearance.from(config);

        assertNull(appearance.customModelData());
        assertNull(appearance.itemModel());
        assertNull(appearance.tooltipStyle());
    }
}
