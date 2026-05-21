package gg.fotia.enchantment.gui.menu;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.List;

public record MenuItemConfig(
        Material material,
        String name,
        List<String> lore,
        Integer modelData,
        String tooltipStyle,
        String itemModel,
        boolean glow
) {

    public static MenuItemConfig from(ConfigurationSection section, Material fallbackMaterial) {
        if (section == null) {
            return new MenuItemConfig(fallbackMaterial, "", Collections.emptyList(), null, null, null, false);
        }

        Material material = parseMaterial(section.getString("material"), fallbackMaterial);
        String name = section.getString("name", "");
        List<String> lore = section.getStringList("lore");
        Integer modelData = readInteger(section, "modeldata", "custom-model-data", "customModelData");
        String tooltipStyle = readString(section, "tooltip-style", "tooltipStyle");
        String itemModel = readString(section, "itemmodel", "item-model", "itemModel");
        boolean glow = section.getBoolean("glow", false);

        return new MenuItemConfig(material, name, List.copyOf(lore), modelData, tooltipStyle, itemModel, glow);
    }

    private static Material parseMaterial(String raw, Material fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(raw);
        return material != null ? material : fallback;
    }

    private static Integer readInteger(ConfigurationSection section, String... keys) {
        for (String key : keys) {
            if (section.contains(key)) {
                return section.getInt(key);
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
