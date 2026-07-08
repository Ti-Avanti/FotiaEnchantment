package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.EnchantmentLimitPolicy;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.core.VanillaManager;
import gg.fotia.enchantment.lore.item.EnchantmentLoreCleaner;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public final class AnvilBreakthroughService {

    private final FotiaEnchantment plugin;

    public AnvilBreakthroughService(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    public Preview preview(Player player, ItemStack target, ItemStack book) {
        if (target == null || target.getType().isAir()) {
            return Preview.failure(FailureReason.MISSING_TARGET);
        }
        if (target.getAmount() != 1) {
            return Preview.failure(FailureReason.TARGET_STACK);
        }
        if (book == null || book.getType() != Material.ENCHANTED_BOOK) {
            return Preview.failure(FailureReason.MISSING_BOOK);
        }

        EnchantmentManager enchantmentManager = plugin.getEnchantmentManager();
        PDCManager pdc = enchantmentManager.getPdcManager();
        Map<String, Integer> incomingCustom = pdc.getEnchantments(book);
        Map<Enchantment, Integer> incomingVanilla = storedVanillaEnchantments(book);
        if (incomingCustom.isEmpty() && incomingVanilla.isEmpty()) {
            return Preview.failure(FailureReason.EMPTY_BOOK);
        }

        ItemStack result = target.clone();
        int max = plugin.getConfigManager().getMaxEnchantmentsForMaterial(result.getType());

        VanillaResult<Enchantment> vanillaMerge = mergeVanillaPreview(result, incomingVanilla, pdc, max);
        if (vanillaMerge.modified()) {
            applyVanillaEnchantments(result, vanillaMerge.enchantments());
        }

        Map<String, Integer> existing = pdc.getEnchantments(result);
        boolean targetIsBook = result.getType() == Material.ENCHANTED_BOOK;
        Result customMerge = mergeCustomEnchantments(
                existing,
                incomingCustom,
                enchantmentManager::getEnchantment,
                data -> pdc.isApplicable(result, data),
                targetIsBook,
                EnchantmentLimitPolicy.countEnchantments(result, pdc),
                max
        );

        if (!vanillaMerge.modified() && !customMerge.modified()) {
            return Preview.failure(FailureReason.NO_VALID_ENCHANTMENT);
        }

        EnchantmentLoreCleaner.stripGeneratedLore(plugin, player, result);
        if (vanillaMerge.modified()) {
            applyVanillaEnchantments(result, vanillaMerge.enchantments());
        }
        for (Map.Entry<String, Integer> entry : customMerge.enchantments().entrySet()) {
            pdc.addEnchantment(result, entry.getKey(), entry.getValue());
        }
        updateDisplay(player, result, customMerge.enchantments(), enchantmentManager);
        return Preview.success(result, customMerge.enchantments());
    }

    public static Result mergeCustomEnchantments(Map<String, Integer> existing,
                                                 Map<String, Integer> incoming,
                                                 Function<String, EnchantmentData> dataResolver,
                                                 Predicate<EnchantmentData> applicable,
                                                 boolean targetIsEnchantedBook,
                                                 int currentEnchantCount,
                                                 int maxEnchantments) {
        AnvilCustomEnchantMerge.Result merge = AnvilCustomEnchantMerge.merge(
                existing,
                incoming,
                dataResolver,
                applicable,
                targetIsEnchantedBook,
                currentEnchantCount,
                maxEnchantments
        );
        return new Result(merge.enchantments(), merge.modified());
    }

    public static <T> VanillaResult<T> mergeVanillaEnchantments(Map<T, Integer> existing,
                                                                Map<T, Integer> incoming,
                                                                Predicate<T> applicable,
                                                                BiPredicate<T, Map<T, Integer>> conflicts,
                                                                ToIntFunction<T> maxLevelResolver,
                                                                int currentEnchantCount,
                                                                int maxEnchantments) {
        Map<T, Integer> result = existing == null ? new HashMap<>() : new HashMap<>(existing);
        if (incoming == null || incoming.isEmpty()) {
            return new VanillaResult(result, false);
        }

        boolean modified = false;
        for (Map.Entry<T, Integer> entry : incoming.entrySet()) {
            T enchantment = entry.getKey();
            int incomingLevel = entry.getValue() == null ? 0 : entry.getValue();
            if (enchantment == null || incomingLevel <= 0 || !applicable.test(enchantment)) {
                continue;
            }
            if (conflicts.test(enchantment, result)) {
                continue;
            }

            int resultLevel = result.getOrDefault(enchantment, 0);
            if (resultLevel <= 0 && currentEnchantCount >= maxEnchantments) {
                continue;
            }

            int firstInputLevel = existing == null ? 0 : existing.getOrDefault(enchantment, 0);
            int mergedLevel = mergeVanillaLevel(firstInputLevel, resultLevel, incomingLevel,
                    Math.max(1, maxLevelResolver.applyAsInt(enchantment)));
            if (mergedLevel > resultLevel) {
                result.put(enchantment, mergedLevel);
                modified = true;
                if (resultLevel <= 0) {
                    currentEnchantCount++;
                }
            }
        }
        return new VanillaResult(result, modified);
    }

    private VanillaResult<Enchantment> mergeVanillaPreview(ItemStack result,
                                                           Map<Enchantment, Integer> incoming,
                                                           PDCManager pdc,
                                                           int maxEnchantments) {
        if (incoming.isEmpty()) {
            return new VanillaResult(vanillaEnchantments(result), false);
        }

        VanillaManager vanillaManager = plugin.getVanillaManager();
        return mergeVanillaEnchantments(
                vanillaEnchantments(result),
                incoming,
                enchantment -> vanillaManager != null
                        && !vanillaManager.isDisabled(enchantment)
                        && vanillaManager.isApplicable(enchantment, result),
                (enchantment, existing) -> vanillaManager != null && vanillaManager.hasAnvilConflict(enchantment, existing),
                enchantment -> vanillaManager == null ? enchantment.getMaxLevel() : vanillaManager.getMaxLevel(enchantment),
                EnchantmentLimitPolicy.countEnchantments(result, pdc),
                maxEnchantments
        );
    }

    private static int mergeVanillaLevel(int firstInputLevel, int resultLevel, int incomingLevel, int maxLevel) {
        int expectedLevel = firstInputLevel == incomingLevel
                ? Math.min(incomingLevel + 1, maxLevel)
                : Math.max(firstInputLevel, incomingLevel);
        return Math.max(Math.max(0, resultLevel), expectedLevel);
    }

    private Map<Enchantment, Integer> vanillaEnchantments(ItemStack item) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) {
            return Map.of();
        }
        Map<Enchantment, Integer> enchantments = new HashMap<>(meta.getEnchants());
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            enchantments.putAll(storageMeta.getStoredEnchants());
        }
        return enchantments;
    }

    private Map<Enchantment, Integer> storedVanillaEnchantments(ItemStack item) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            return new HashMap<>(storageMeta.getStoredEnchants());
        }
        return Map.of();
    }

    private void applyVanillaEnchantments(ItemStack item, Map<Enchantment, Integer> enchantments) {
        ItemMeta meta = item == null ? null : item.getItemMeta();
        if (meta == null) {
            return;
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                storageMeta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
            }
        } else {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }
        item.setItemMeta(meta);
    }

    private void updateDisplay(Player player,
                               ItemStack result,
                               Map<String, Integer> enchantments,
                               EnchantmentManager enchantmentManager) {
        if (result.getType() == Material.ENCHANTED_BOOK && enchantments.size() == 1
                && plugin.getCustomItemManager() != null
                && plugin.getCustomItemManager().getStellarisCodex() != null) {
            Map.Entry<String, Integer> entry = enchantments.entrySet().iterator().next();
            EnchantmentData data = enchantmentManager.getEnchantment(entry.getKey());
            if (data != null) {
                plugin.getCustomItemManager().getStellarisCodex()
                        .updateEnchantedBookDisplay(player, result, data, entry.getValue());
                return;
            }
        }
        EnchantmentLoreCleaner.applyGeneratedLore(plugin, player, result);
    }

    public record Result(Map<String, Integer> enchantments, boolean modified) {
    }

    public record VanillaResult<T>(Map<T, Integer> enchantments, boolean modified) {
    }

    public record Preview(boolean success,
                          ItemStack result,
                          Map<String, Integer> enchantments,
                          FailureReason failureReason) {

        static Preview success(ItemStack result, Map<String, Integer> enchantments) {
            return new Preview(true, result, Map.copyOf(enchantments), null);
        }

        static Preview failure(FailureReason reason) {
            return new Preview(false, null, Map.of(), reason);
        }
    }

    public enum FailureReason {
        MISSING_TARGET,
        TARGET_STACK,
        MISSING_BOOK,
        EMPTY_BOOK,
        NO_VALID_ENCHANTMENT
    }
}
