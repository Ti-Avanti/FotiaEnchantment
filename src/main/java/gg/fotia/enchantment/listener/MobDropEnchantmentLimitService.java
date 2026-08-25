package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentRegistry;
import gg.fotia.enchantment.core.MobDropEnchantmentLimitPolicy;
import gg.fotia.enchantment.core.MobDropOverflowAction;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.lore.item.EnchantmentLoreCleaner;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class MobDropEnchantmentLimitService {

    enum Outcome {
        UNCHANGED,
        TRIMMED,
        REMOVE_DROP
    }

    private final FotiaEnchantment plugin;
    private final PDCManager pdc;

    MobDropEnchantmentLimitService(FotiaEnchantment plugin) {
        this.plugin = plugin;
        this.pdc = plugin.getEnchantmentManager().getPdcManager();
    }

    Outcome enforce(ItemStack item, int max, MobDropOverflowAction action) {
        if (item == null || item.getType().isAir() || max < 0 || !item.hasItemMeta()) {
            return Outcome.UNCHANGED;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Outcome.UNCHANGED;
        }

        Map<String, Integer> customEnchantments = new HashMap<>(pdc.getEnchantments(meta));
        Map<String, Integer> allEnchantments = collectEnchantments(meta, customEnchantments);
        Set<String> removals = MobDropEnchantmentLimitPolicy.keysToRemove(allEnchantments, max);
        if (removals.isEmpty()) {
            return Outcome.UNCHANGED;
        }
        if (action == MobDropOverflowAction.REMOVE_DROP) {
            return Outcome.REMOVE_DROP;
        }

        removeCustomEnchantments(item, customEnchantments, removals);
        removeNativeEnchantments(item, removals);
        EnchantmentLoreCleaner.applyGeneratedLore(plugin, null, item);
        return Outcome.TRIMMED;
    }

    private Map<String, Integer> collectEnchantments(ItemMeta meta,
                                                     Map<String, Integer> customEnchantments) {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, Integer> entry : customEnchantments.entrySet()) {
            mergeLevel(result, customKey(entry.getKey()), entry.getValue());
        }
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            mergeNativeLevel(result, entry.getKey(), entry.getValue());
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<Enchantment, Integer> entry : storageMeta.getStoredEnchants().entrySet()) {
                mergeNativeLevel(result, entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private void mergeNativeLevel(Map<String, Integer> result, Enchantment enchantment, Integer level) {
        if (enchantment == null || isFotiaEnchantment(enchantment)) {
            return;
        }
        NamespacedKey key = enchantment.getKey();
        String id = key != null ? key.toString() : enchantment.toString().toLowerCase(Locale.ROOT);
        mergeLevel(result, id, level);
    }

    private static void mergeLevel(Map<String, Integer> result, String key, Integer level) {
        if (key == null || key.isBlank() || level == null || level <= 0) {
            return;
        }
        result.merge(key, level, Math::max);
    }

    private void removeCustomEnchantments(ItemStack item,
                                          Map<String, Integer> customEnchantments,
                                          Set<String> removals) {
        boolean changed = customEnchantments.keySet().removeIf(id -> removals.contains(customKey(id)));
        if (changed) {
            pdc.setEnchantments(item, customEnchantments);
        }
    }

    private void removeNativeEnchantments(ItemStack item, Set<String> removals) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        boolean changed = false;
        for (Enchantment enchantment : meta.getEnchants().keySet().toArray(Enchantment[]::new)) {
            if (!isFotiaEnchantment(enchantment) && removals.contains(nativeKey(enchantment))) {
                changed |= meta.removeEnchant(enchantment);
            }
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Enchantment enchantment : storageMeta.getStoredEnchants().keySet().toArray(Enchantment[]::new)) {
                if (!isFotiaEnchantment(enchantment) && removals.contains(nativeKey(enchantment))) {
                    changed |= storageMeta.removeStoredEnchant(enchantment);
                }
            }
        }
        if (changed) {
            item.setItemMeta(meta);
        }
    }

    private static String customKey(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        return normalized.contains(":")
                ? normalized
                : EnchantmentRegistry.getNamespace() + ":" + normalized;
    }

    private static String nativeKey(Enchantment enchantment) {
        NamespacedKey key = enchantment.getKey();
        return key != null ? key.toString() : enchantment.toString().toLowerCase(Locale.ROOT);
    }

    private static boolean isFotiaEnchantment(Enchantment enchantment) {
        NamespacedKey key = enchantment.getKey();
        return key != null && EnchantmentRegistry.getNamespace().equals(key.getNamespace());
    }
}
