package gg.fotia.enchantment.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
