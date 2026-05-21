package gg.fotia.enchantment.item;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CodexRarityWeightsTest {

    @Test
    void configWeightsOverrideRarityFallback() {
        YamlConfiguration mainConfig = new YamlConfiguration();
        mainConfig.set("stellaris-codex.rarity-weights.dustlight", 5);
        mainConfig.set("stellaris-codex.rarity-weights.divine", 95);
        YamlConfiguration rarityConfig = rarityConfig();

        Map<String, Integer> weights = CodexRarityWeights.resolve(mainConfig, rarityConfig);

        assertEquals(Map.of("dustlight", 5, "divine", 95), weights);
    }

    @Test
    void missingConfigWeightsUseRarityCodexRollChance() {
        Map<String, Integer> weights = CodexRarityWeights.resolve(new YamlConfiguration(), rarityConfig());

        assertEquals(Map.of("dustlight", 40, "moonlit", 30, "radiant", 18), weights);
    }

    @Test
    void invalidConfigWeightsFallBackToRarityConfig() {
        YamlConfiguration mainConfig = new YamlConfiguration();
        mainConfig.set("stellaris-codex.rarity-weights.dustlight", 0);
        mainConfig.set("stellaris-codex.rarity-weights.divine", -5);

        Map<String, Integer> weights = CodexRarityWeights.resolve(mainConfig, rarityConfig());

        assertEquals(Map.of("dustlight", 40, "moonlit", 30, "radiant", 18), weights);
    }

    private YamlConfiguration rarityConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("dustlight.codex-roll-chance", 40);
        config.set("moonlit.codex-roll-chance", 30);
        config.set("radiant.codex-roll-chance", 18);
        config.set("aureate.codex-roll-chance", 0);
        return config;
    }
}
