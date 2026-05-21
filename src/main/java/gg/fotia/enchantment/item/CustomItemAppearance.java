package gg.fotia.enchantment.item;

import org.bukkit.configuration.ConfigurationSection;

public record CustomItemAppearance(Integer customModelData, String itemModel, String tooltipStyle) {

    public static CustomItemAppearance from(ConfigurationSection section) {
        if (section == null) {
            return new CustomItemAppearance(null, null, null);
        }
        return new CustomItemAppearance(
                readInteger(section, "modeldata", "custom-model-data", "customModelData"),
                readString(section, "itemmodel", "item-model", "itemModel"),
                readString(section, "tooltip-style", "tooltipStyle")
        );
    }

    private static Integer readInteger(ConfigurationSection section, String... keys) {
        for (String key : keys) {
            if (section.contains(key)) {
                int value = section.getInt(key, 0);
                return value == 0 ? null : value;
            }
        }
        return null;
    }

    private static String readString(ConfigurationSection section, String... keys) {
        for (String key : keys) {
            String value = section.getString(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
