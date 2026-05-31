package gg.fotia.enchantment.core;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.lore.item.EnchantmentDisplayPolicy;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class EnchantmentItemSanitizer {

    private EnchantmentItemSanitizer() {
    }

    public static boolean sanitize(FotiaEnchantment plugin, ItemStack item) {
        if (plugin == null || item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }

        EnchantmentManager manager = plugin.getEnchantmentManager();
        if (manager == null) {
            return false;
        }

        PDCManager pdc = manager.getPdcManager();
        Map<String, Integer> existing = pdc.getEnchantments(item);
        if (existing.isEmpty()) {
            return false;
        }

        Map<String, Integer> valid = validEnchantments(
                existing,
                item.getType(),
                ValidityRules.from(manager.getAllEnchantments())
        );
        if (existing.equals(valid)) {
            return false;
        }

        for (String enchantId : existing.keySet()) {
            pdc.removeEnchantment(item, enchantId);
        }
        for (Map.Entry<String, Integer> entry : valid.entrySet()) {
            pdc.addEnchantment(item, entry.getKey(), entry.getValue());
        }
        return true;
    }

    public static boolean requiresNormalization(ItemStack item, PDCManager pdc, ValidityRules rules) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (pdc != null && rules != null) {
            if (needsSanitization(item, pdc, rules)) {
                return true;
            }
        }

        boolean hasStoredEnchants = meta instanceof EnchantmentStorageMeta storageMeta
                && !storageMeta.getStoredEnchants().isEmpty();
        boolean hasLegacyCustomEnchants = pdc != null && !pdc.getLegacyEnchantments(item).isEmpty();
        return EnchantmentDisplayPolicy.shouldHideNativeEnchantments(
                meta.hasEnchants(),
                hasStoredEnchants,
                hasLegacyCustomEnchants
        );
    }

    public static boolean needsSanitization(ItemStack item, PDCManager pdc, ValidityRules rules) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta() || pdc == null || rules == null) {
            return false;
        }
        Map<String, Integer> existing = pdc.getEnchantments(item);
        return !existing.equals(validEnchantments(existing, item.getType(), rules));
    }

    public static Map<String, Integer> validEnchantments(Map<String, Integer> existing,
                                                         Material material,
                                                         ValidityRules rules) {
        if (existing == null || existing.isEmpty() || rules == null) {
            return Map.of();
        }

        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : existing.entrySet()) {
            String id = normalizeId(entry.getKey());
            int level = entry.getValue() == null ? 0 : entry.getValue();
            EnchantmentRule rule = rules.rule(id);
            if (!isValid(rule, material, level)) {
                continue;
            }
            result.merge(id, level, Math::max);
        }
        return result;
    }

    public static boolean isValid(EnchantmentData data, Material material, int level) {
        if (data == null) {
            return false;
        }
        return isValid(EnchantmentRule.from(data), material, level);
    }

    private static boolean isValid(EnchantmentRule rule, Material material, int level) {
        if (rule == null || !rule.enabled() || level <= 0 || rule.maxLevel() <= 0) {
            return false;
        }
        if (material == Material.ENCHANTED_BOOK) {
            return true;
        }
        return rule.applicableItems().isEmpty() || rule.applicableItems().contains(material);
    }

    private static String normalizeId(String id) {
        if (id == null) {
            return "";
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(':');
        if (separator > 0 && separator < normalized.length() - 1
                && EnchantmentRegistry.getNamespace().equals(normalized.substring(0, separator))) {
            return normalized.substring(separator + 1);
        }
        return normalized;
    }

    public record ValidityRules(Map<String, EnchantmentRule> rules) {
        public static ValidityRules from(Collection<EnchantmentData> enchantments) {
            if (enchantments == null || enchantments.isEmpty()) {
                return new ValidityRules(Map.of());
            }
            Map<String, EnchantmentRule> rules = new LinkedHashMap<>();
            for (EnchantmentData data : enchantments) {
                if (data == null || data.getId() == null || data.getId().isBlank()) {
                    continue;
                }
                rules.put(normalizeId(data.getId()), EnchantmentRule.from(data));
            }
            return new ValidityRules(Map.copyOf(rules));
        }

        EnchantmentRule rule(String id) {
            return rules.get(normalizeId(id));
        }
    }

    public record EnchantmentRule(boolean enabled, int maxLevel, Set<Material> applicableItems) {
        static EnchantmentRule from(EnchantmentData data) {
            Set<Material> applicableItems = data.getApplicableItems() == null
                    ? Set.of()
                    : data.getApplicableItems().stream().collect(Collectors.toUnmodifiableSet());
            return new EnchantmentRule(data.isEnabled(), data.getMaxLevel(), applicableItems);
        }
    }
}
