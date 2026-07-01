package gg.fotia.enchantment.item;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class DisenchantItemRegistry {

    private static final String ITEMS_PATH = "disenchant-stone.items";
    private static final String LEGACY_TIERS_PATH = "disenchant-stone.tiers";
    private static final Map<String, String> LEGACY_TIER_TO_ITEM = Map.of(
            "tier-1", "disenchant-shard",
            "tier-2", "disenchant-crystal",
            "tier-3", "disenchant-gem"
    );
    private static final Map<String, String> LEGACY_ITEM_TO_TIER = legacyItemToTier();

    private DisenchantItemRegistry() {
    }

    public static Set<String> itemIds(YamlConfiguration config) {
        Set<String> result = new LinkedHashSet<>();
        if (config == null) {
            return result;
        }

        ConfigurationSection items = config.getConfigurationSection(ITEMS_PATH);
        if (items != null) {
            result.addAll(items.getKeys(false));
        }

        ConfigurationSection tiers = config.getConfigurationSection(LEGACY_TIERS_PATH);
        if (tiers != null) {
            for (String tier : tiers.getKeys(false)) {
                result.add(itemIdForConfigKey(config, tier));
            }
        }
        return result;
    }

    public static String configKey(YamlConfiguration config, String itemId) {
        if (config == null || itemId == null || itemId.isBlank()) {
            return null;
        }
        if (config.isConfigurationSection(ITEMS_PATH + "." + itemId)) {
            return itemId;
        }
        String legacyTier = LEGACY_ITEM_TO_TIER.get(itemId);
        if (legacyTier != null && config.isConfigurationSection(LEGACY_TIERS_PATH + "." + legacyTier)) {
            return legacyTier;
        }
        return null;
    }

    public static String itemIdForConfigKey(YamlConfiguration config, String configKey) {
        if (configKey == null || configKey.isBlank()) {
            return null;
        }
        return LEGACY_TIER_TO_ITEM.getOrDefault(configKey, configKey);
    }

    public static ConfigurationSection section(YamlConfiguration config, String itemId) {
        String configKey = configKey(config, itemId);
        if (configKey == null) {
            return null;
        }
        return sectionForConfigKey(config, configKey);
    }

    public static ConfigurationSection sectionForConfigKey(YamlConfiguration config, String configKey) {
        if (config == null || configKey == null || configKey.isBlank()) {
            return null;
        }
        if (config.isConfigurationSection(ITEMS_PATH + "." + configKey)) {
            return config.getConfigurationSection(ITEMS_PATH + "." + configKey);
        }
        return config.getConfigurationSection(LEGACY_TIERS_PATH + "." + configKey);
    }

    public static DisenchantSource source(ConfigurationSection section) {
        if (section == null) {
            return DisenchantSource.FOTIA;
        }
        return DisenchantSource.fromConfig(section.getString("source", "FOTIA"));
    }

    public static boolean isDisenchantItem(YamlConfiguration config, String itemId) {
        return configKey(config, itemId) != null;
    }

    private static Map<String, String> legacyItemToTier() {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : LEGACY_TIER_TO_ITEM.entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }
        return Map.copyOf(result);
    }
}
