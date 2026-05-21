package gg.fotia.enchantment.gui.menu;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

public record MenuConfig(
        int rows,
        String title,
        boolean fillEmpty,
        List<Integer> enchantmentSlots,
        MenuLayout layout,
        ConfigurationSection root
) {

    public static MenuConfig from(YamlConfiguration yaml, String menuId) {
        ConfigurationSection root = yaml.getConfigurationSection("menus." + menuId);
        if (root == null) {
            root = yaml;
        }

        MenuLayout layout = MenuLayout.from(root);
        int rowFallback = layout.rows() > 0 ? layout.rows() : 6;
        int rows = Math.max(1, Math.min(6, root.getInt("rows", rowFallback)));
        String title = root.getString("title", "lang:admin-gui.title");
        boolean fillEmpty = root.getBoolean("fill-empty", true);
        Object enchantmentSlotConfig = root.contains("layout.enchantments")
                ? root.get("layout.enchantments")
                : List.of("9-44");
        List<Integer> enchantmentSlots = roleSlots(root, layout, "enchantments", MenuSlots.parse(enchantmentSlotConfig));

        return new MenuConfig(rows, title, fillEmpty, enchantmentSlots, layout, root);
    }

    public MenuItemConfig item(String itemId, Material fallbackMaterial) {
        return MenuItemConfig.from(root.getConfigurationSection("items." + itemId), fallbackMaterial);
    }

    public int slot(String path, int fallback) {
        return root.getInt(path, fallback);
    }

    public List<Integer> slots(String path, List<?> fallback) {
        Object raw = root.contains(path) ? root.get(path) : fallback;
        return MenuSlots.parse(raw);
    }

    public int roleSlot(String role, int fallback) {
        List<Integer> slots = roleSlots(role, List.of(fallback));
        return slots.isEmpty() ? fallback : slots.get(0);
    }

    public List<Integer> roleSlots(String role, List<Integer> fallback) {
        return roleSlots(root, layout, role, fallback);
    }

    private static List<Integer> roleSlots(
            ConfigurationSection root,
            MenuLayout layout,
            String role,
            List<Integer> fallback
    ) {
        String symbols = root.getString("roles." + role);
        List<Integer> layoutSlots = layout.slots(symbols);
        if (!layoutSlots.isEmpty()) {
            return layoutSlots;
        }
        Object legacy = legacyRoleConfig(root, role);
        List<Integer> legacySlots = MenuSlots.parse(legacy);
        if (!legacySlots.isEmpty()) {
            return legacySlots;
        }
        return fallback == null ? List.of() : fallback;
    }

    private static Object legacyRoleConfig(ConfigurationSection root, String role) {
        if (root.contains("layout." + role)) {
            return root.get("layout." + role);
        }
        if (role.startsWith("category-")) {
            String category = role.substring("category-".length());
            return root.get("layout.categories." + category);
        }
        return switch (role) {
            case "previous-page" -> root.get("layout.controls.previous");
            case "next-page" -> root.get("layout.controls.next");
            case "close" -> root.get("layout.controls.close");
            default -> null;
        };
    }
}
