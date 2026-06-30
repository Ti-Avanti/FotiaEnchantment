package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.EnchantmentLimitPolicy;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.lore.item.EnchantmentLoreCleaner;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

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
        Map<String, Integer> incoming = pdc.getEnchantments(book);
        if (incoming.isEmpty()) {
            return Preview.failure(FailureReason.EMPTY_BOOK);
        }

        ItemStack result = target.clone();
        Map<String, Integer> existing = pdc.getEnchantments(result);
        int max = plugin.getConfigManager().getMaxEnchantmentsForMaterial(result.getType());
        boolean targetIsBook = result.getType() == Material.ENCHANTED_BOOK;
        Result merged = mergeCustomEnchantments(
                existing,
                incoming,
                enchantmentManager::getEnchantment,
                data -> pdc.isApplicable(result, data),
                targetIsBook,
                EnchantmentLimitPolicy.countEnchantments(result, pdc),
                max
        );

        if (!merged.modified()) {
            return Preview.failure(FailureReason.NO_VALID_ENCHANTMENT);
        }

        EnchantmentLoreCleaner.stripGeneratedLore(plugin, player, result);
        for (Map.Entry<String, Integer> entry : merged.enchantments().entrySet()) {
            pdc.addEnchantment(result, entry.getKey(), entry.getValue());
        }
        updateDisplay(player, result, merged.enchantments(), enchantmentManager);
        return Preview.success(result, merged.enchantments());
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
