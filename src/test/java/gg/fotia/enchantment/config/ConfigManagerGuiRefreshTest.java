package gg.fotia.enchantment.config;

import org.junit.jupiter.api.Test;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerGuiRefreshTest {

    @Test
    void insertsDescriptionLinesBeforeEffectLinesForOldGuideLore() {
        List<String> refreshed = ConfigManager.refreshEnchantmentGuideLore(List.of(
                "lang:guide-gui.rarity-line",
                "{curse_line}",
                "",
                "{effect_lines}"
        ));

        assertEquals(List.of(
                "lang:guide-gui.rarity-line",
                "{curse_line}",
                "",
                "{description_lines}",
                "",
                "{effect_lines}"
        ), refreshed);
    }

    @Test
    void replacesOldTriggerLinesWithDescriptionLines() {
        List<String> refreshed = ConfigManager.refreshEnchantmentGuideLore(List.of(
                "lang:guide-gui.rarity-line",
                "{trigger_lines}"
        ));

        assertEquals(List.of(
                "lang:guide-gui.rarity-line",
                "{description_lines}"
        ), refreshed);
    }

    @Test
    void leavesCurrentGuideLoreUnchanged() {
        List<String> current = List.of(
                "lang:guide-gui.rarity-line",
                "",
                "{description_lines}",
                "",
                "{effect_lines}"
        );

        assertTrue(ConfigManager.refreshEnchantmentGuideLore(current) == current);
    }

    @Test
    void oldLimitsConfigGainsSpearGroupFromTridents() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("item-groups.tridents", 6);

        assertTrue(ConfigManager.refreshLimitsConfig(config));
        assertEquals(6, config.getInt("item-groups.spears"));
    }

    @Test
    void customSpearGroupLimitIsPreserved() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("item-groups.tridents", 6);
        config.set("item-groups.spears", 4);

        assertFalse(ConfigManager.refreshLimitsConfig(config));
        assertEquals(4, config.getInt("item-groups.spears"));
    }

    @Test
    void oldCustomItemsConfigGainsAnvilBreakthroughStone() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("stellaris-codex.preview.material", "ENCHANTED_BOOK");

        assertTrue(ConfigManager.refreshCustomItemsConfig(config));
        assertEquals("ECHO_SHARD", config.getString("anvil-breakthrough-stone.material"));
        assertEquals(10030, config.getInt("anvil-breakthrough-stone.custom-model-data"));
        assertEquals("", config.getString("anvil-breakthrough-stone.item-model"));
        assertEquals("", config.getString("anvil-breakthrough-stone.tooltip-style"));
        assertEquals("", config.getString("anvil-breakthrough-stone.craftengine-item"));
        assertTrue(config.getBoolean("anvil-breakthrough-stone.glow"));
    }

    @Test
    void customAnvilBreakthroughStoneConfigIsPreserved() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("anvil-breakthrough-stone.material", "AMETHYST_SHARD");
        config.set("anvil-breakthrough-stone.custom-model-data", 7);

        assertFalse(ConfigManager.refreshCustomItemsConfig(config));
        assertEquals("AMETHYST_SHARD", config.getString("anvil-breakthrough-stone.material"));
        assertEquals(7, config.getInt("anvil-breakthrough-stone.custom-model-data"));
    }
}
