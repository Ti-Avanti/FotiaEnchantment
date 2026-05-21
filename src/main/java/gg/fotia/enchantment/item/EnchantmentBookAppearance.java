package gg.fotia.enchantment.item;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public record EnchantmentBookAppearance(
        Integer customModelData,
        String itemModel,
        String tooltipStyle
) {

    public static EnchantmentBookAppearance from(YamlConfiguration config, String rarity) {
        ConfigurationSection defaults = config != null ? config.getConfigurationSection("default") : null;
        ConfigurationSection raritySection = config != null && rarity != null
                ? config.getConfigurationSection(rarity)
                : null;

        Integer customModelData = readInteger(raritySection, "modeldata", "custom-model-data", "customModelData");
        if (customModelData == null) {
            customModelData = readInteger(defaults, "modeldata", "custom-model-data", "customModelData");
        }

        String itemModel = readString(raritySection, "itemmodel", "item-model", "itemModel");
        if (itemModel == null) {
            itemModel = readString(defaults, "itemmodel", "item-model", "itemModel");
        }

        String tooltipStyle = readString(raritySection, "tooltip-style", "tooltipStyle");
        if (tooltipStyle == null) {
            tooltipStyle = readString(defaults, "tooltip-style", "tooltipStyle");
        }

        return new EnchantmentBookAppearance(customModelData, itemModel, tooltipStyle);
    }

    private static Integer readInteger(ConfigurationSection section, String... keys) {
        if (section == null) {
            return null;
        }
        for (String key : keys) {
            if (section.contains(key)) {
                int value = section.getInt(key);
                return value > 0 ? value : null;
            }
        }
        return null;
    }

    private static String readString(ConfigurationSection section, String... keys) {
        if (section == null) {
            return null;
        }
        for (String key : keys) {
            String value = section.getString(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
