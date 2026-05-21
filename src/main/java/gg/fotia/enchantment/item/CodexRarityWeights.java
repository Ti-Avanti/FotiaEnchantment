package gg.fotia.enchantment.item;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CodexRarityWeights {

    private static final String CONFIG_PATH = "stellaris-codex.rarity-weights";
    private static final String FALLBACK_WEIGHT_KEY = "codex-roll-chance";

    private CodexRarityWeights() {
    }

    public static Map<String, Integer> resolve(YamlConfiguration mainConfig, YamlConfiguration rarityConfig) {
        Map<String, Integer> configured = readSection(mainConfig == null ? null : mainConfig.getConfigurationSection(CONFIG_PATH), null);
        if (!configured.isEmpty()) {
            return configured;
        }
        return readSection(rarityConfig, FALLBACK_WEIGHT_KEY);
    }

    private static Map<String, Integer> readSection(ConfigurationSection section, String nestedWeightKey) {
        Map<String, Integer> weights = new LinkedHashMap<>();
        if (section == null) {
            return weights;
        }

        for (String key : section.getKeys(false)) {
            int weight = nestedWeightKey == null
                    ? section.getInt(key, 0)
                    : section.getInt(key + "." + nestedWeightKey, 0);
            if (weight > 0) {
                weights.put(key, weight);
            }
        }
        return weights;
    }
}
