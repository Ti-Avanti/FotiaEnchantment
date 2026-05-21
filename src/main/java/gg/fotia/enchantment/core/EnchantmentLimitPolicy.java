package gg.fotia.enchantment.core;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class EnchantmentLimitPolicy {

    private EnchantmentLimitPolicy() {
    }

    public static int resolveLimit(YamlConfiguration config, Material material, int fallbackDefault) {
        int defaultLimit = config != null
                ? config.getInt("default-max-enchantments", fallbackDefault)
                : fallbackDefault;
        if (material == null) {
            return defaultLimit;
        }

        String materialName = material.name();
        if (config != null && config.contains("materials." + materialName)) {
            return config.getInt("materials." + materialName, defaultLimit);
        }

        String group = groupFor(material);
        if (group != null && config != null && config.contains("item-groups." + group)) {
            return config.getInt("item-groups." + group, defaultLimit);
        }
        return defaultLimit;
    }

    public static boolean canAddNewEnchantment(int currentCount, int max) {
        return max < 0 || currentCount < max;
    }

    public static boolean isLimitExceeded(int count, int max) {
        return max >= 0 && count > max;
    }

    public static boolean canAddEnchantment(ItemStack item, PDCManager pdc, String enchantId, int max) {
        if (max < 0) {
            return true;
        }
        if (item == null || enchantId == null || enchantId.isBlank()) {
            return false;
        }
        if (containsEnchantment(item, pdc, enchantId)) {
            return true;
        }
        return canAddNewEnchantment(countEnchantments(item, pdc), max);
    }

    public static int countEnchantments(ItemStack item, PDCManager pdc) {
        return collectEnchantmentKeys(item, pdc).size();
    }

    public static int countEnchantments(ItemStack item,
                                        PDCManager pdc,
                                        Map<Enchantment, Integer> nativeAdds) {
        Set<String> keys = collectEnchantmentKeys(item, pdc);
        if (nativeAdds != null) {
            for (Map.Entry<Enchantment, Integer> entry : nativeAdds.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) {
                    keys.add(toKey(entry.getKey()));
                }
            }
        }
        return keys.size();
    }

    private static boolean containsEnchantment(ItemStack item, PDCManager pdc, String enchantId) {
        String normalized = normalizeCustomKey(enchantId);
        return collectEnchantmentKeys(item, pdc).contains(normalized);
    }

    private static Set<String> collectEnchantmentKeys(ItemStack item, PDCManager pdc) {
        Set<String> result = new HashSet<>();
        if (item == null || !item.hasItemMeta()) {
            return result;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return result;
        }
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                result.add(toKey(entry.getKey()));
            }
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<Enchantment, Integer> entry : storageMeta.getStoredEnchants().entrySet()) {
                if (entry.getValue() != null && entry.getValue() > 0) {
                    result.add(toKey(entry.getKey()));
                }
            }
        }
        if (pdc != null) {
            for (String id : pdc.getLegacyEnchantments(item).keySet()) {
                result.add(normalizeCustomKey(id));
            }
        }
        return result;
    }

    private static String normalizeCustomKey(String enchantId) {
        String id = enchantId.toLowerCase(Locale.ROOT);
        if (id.contains(":")) {
            return id;
        }
        Enchantment trueEnchantment = Registry.ENCHANTMENT.get(new NamespacedKey(
                EnchantmentRegistry.getNamespace(),
                id));
        if (trueEnchantment != null) {
            return toKey(trueEnchantment);
        }
        return EnchantmentRegistry.getNamespace() + ":" + id;
    }

    private static String toKey(Enchantment enchantment) {
        NamespacedKey key = enchantment.getKey();
        return key == null ? enchantment.toString().toLowerCase(Locale.ROOT) : key.toString().toLowerCase(Locale.ROOT);
    }

    private static String groupFor(Material material) {
        String name = material.name();
        if (name.endsWith("_HELMET") || name.equals("TURTLE_HELMET")) return "helmets";
        if (name.endsWith("_CHESTPLATE") || name.equals("ELYTRA")) return "chestplates";
        if (name.endsWith("_LEGGINGS")) return "leggings";
        if (name.endsWith("_BOOTS")) return "boots";
        if (name.endsWith("_SWORD")) return "swords";
        if (name.endsWith("_AXE")) return "axes";
        if (name.endsWith("_PICKAXE")) return "pickaxes";
        if (name.endsWith("_SHOVEL")) return "shovels";
        if (name.endsWith("_HOE")) return "hoes";
        if (name.equals("BOW")) return "bows";
        if (name.equals("CROSSBOW")) return "crossbows";
        if (name.equals("TRIDENT")) return "tridents";
        if (name.equals("FISHING_ROD")) return "fishing-rods";
        if (name.equals("SHIELD")) return "shields";
        if (name.equals("BOOK") || name.equals("ENCHANTED_BOOK")) return "books";
        return null;
    }
}
