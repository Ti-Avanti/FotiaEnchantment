package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantingTableLevelPolicy;
import gg.fotia.enchantment.core.EnchantmentConflictPolicy;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.EnchantmentLimitPolicy;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.core.EnchantmentRegistry;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.lore.item.EnchantmentLoreCleaner;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class EnchantListener implements Listener {

    private final FotiaEnchantment plugin;

    public EnchantListener(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        if (!plugin.getConfigManager().isEnchantingTableEnabled()) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        EnchantmentManager enchantManager = plugin.getEnchantmentManager();
        PDCManager pdc = enchantManager.getPdcManager();
        if (!pdc.getEnchantments(item).isEmpty()) {
            return;
        }

        List<EnchantmentData> applicable = new ArrayList<>();
        for (EnchantmentData data : enchantManager.getEnabled()) {
            if (!data.getObtain().isEnchantingTable()) {
                continue;
            }
            if (!isEnchantingTableBook(item) && !pdc.isApplicable(item, data)) {
                continue;
            }
            if (data.getObtain().getEnchantingTableWeight() <= 0) {
                continue;
            }
            applicable.add(data);
        }
        if (applicable.isEmpty()) {
            return;
        }

        int cost = event.getExpLevelCost();
        int attempts = EnchantingTablePolicy.customRollAttempts(
                cost,
                plugin.getConfigManager().getEnchantingTableCustomRolls());
        if (attempts <= 0) {
            return;
        }

        double pickChance = EnchantingTablePolicy.customRollChance(
                cost,
                plugin.getConfigManager().getEnchantingTableBaseChance(),
                plugin.getConfigManager().getEnchantingTableChancePerLevel(),
                plugin.getConfigManager().getEnchantingTableMaxChance());

        int max = plugin.getConfigManager().getMaxEnchantmentsForMaterial(item.getType());
        int existingCount = EnchantmentLimitPolicy.countEnchantments(item, pdc);
        EnchantmentLimitPolicy.trimPendingEnchantmentsToLimit(
                event.getEnchantsToAdd(),
                existingCount,
                max,
                event.getEnchantmentHint());
        Set<String> selectedIds = selectedCustomIds(event.getEnchantsToAdd(), pdc.getEnchantments(item));
        int currentCount = EnchantmentLimitPolicy.countEnchantments(item, pdc, event.getEnchantsToAdd());
        if (!EnchantmentLimitPolicy.canAddNewEnchantment(currentCount, max)) {
            return;
        }

        List<PendingCustomEnchant> pdcOnlyAdds = new ArrayList<>();
        for (int i = 0; i < attempts && EnchantmentLimitPolicy.canAddNewEnchantment(currentCount, max); i++) {
            if (ThreadLocalRandom.current().nextDouble() >= pickChance) {
                continue;
            }

            EnchantmentData picked = pickWeighted(applicable, selectedIds, enchantManager, pdc, item);
            if (picked == null) {
                break;
            }

            int level = customEnchantingTableLevel(picked, cost, i);
            selectedIds.add(picked.getId());
            currentCount++;

            Enchantment trueEnchantment = resolveTrueEnchantment(picked.getId());
            if (trueEnchantment != null) {
                event.getEnchantsToAdd().put(trueEnchantment, level);
            } else {
                pdcOnlyAdds.add(new PendingCustomEnchant(picked.getId(), level));
            }
        }

        if (!pdcOnlyAdds.isEmpty()) {
            ItemStack modified = item.clone();
            for (PendingCustomEnchant pending : pdcOnlyAdds) {
                pdc.addEnchantment(modified, pending.id(), pending.level());
            }
            EnchantmentLoreCleaner.stripGeneratedLore(plugin, event.getEnchanter(), modified);
            event.setItem(modified);
        }
    }

    private int customEnchantingTableLevel(EnchantmentData data, int cost, int rollIndex) {
        int maxLevel = Math.max(1, data.getMaxLevel());
        if (plugin.getConfigManager() == null || !plugin.getConfigManager().isEnchantingTableLevelRollEnabled()) {
            return EnchantingTableLevelPolicy.legacyScaledLevel(maxLevel, cost);
        }
        return EnchantingTableLevelPolicy.rollLevel(
                maxLevel,
                cost,
                ThreadLocalRandom.current().nextInt(),
                data.getId(),
                rollIndex,
                plugin.getConfigManager().getEnchantingTableLevelTiers());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!plugin.getConfigManager().isAnvilEnabled()) {
            return;
        }

        AnvilInventory inv = event.getInventory();
        ItemStack first = inv.getItem(0);
        ItemStack second = inv.getItem(1);
        if (first == null || second == null) {
            return;
        }

        EnchantmentManager enchantManager = plugin.getEnchantmentManager();
        PDCManager pdc = enchantManager.getPdcManager();

        AnvilMergeInput inputs = anvilMergeInput(first, second, event.getResult());
        if (inputs == null) {
            return;
        }
        Map<String, Integer> sourceEnchants = pdc.getEnchantments(inputs.source());
        if (sourceEnchants.isEmpty()) {
            return;
        }

        ItemStack result = inputs.result();

        ItemStack mergeTarget = inputs.target();
        int max = plugin.getConfigManager().getMaxEnchantmentsForMaterial(mergeTarget.getType());
        Map<String, Integer> existing = pdc.getEnchantments(mergeTarget);
        AnvilCustomEnchantMerge.Result merge = AnvilCustomEnchantMerge.merge(
                existing,
                sourceEnchants,
                enchantManager::getEnchantment,
                data -> pdc.isApplicable(mergeTarget, data),
                mergeTarget.getType() == Material.ENCHANTED_BOOK,
                EnchantmentLimitPolicy.countEnchantments(mergeTarget, pdc),
                max
        );
        if (merge.modified()) {
            for (Map.Entry<String, Integer> entry : merge.enchantments().entrySet()) {
                pdc.addEnchantment(result, entry.getKey(), entry.getValue());
            }
            HumanEntity viewer = event.getView().getPlayer();
            Player player = viewer instanceof Player p ? p : null;
            if (!updateSingleBookDisplay(player, result, merge.enchantments(), enchantManager)) {
                EnchantmentLoreCleaner.applyGeneratedLore(plugin, player, result);
            }
            event.setResult(result);
            event.getView().setRepairCost(Math.max(1, event.getView().getRepairCost()));
        }
    }

    private AnvilMergeInput anvilMergeInput(ItemStack first, ItemStack second, ItemStack currentResult) {
        if (first == null || second == null || first.getType().isAir() || second.getType().isAir()) {
            return null;
        }

        ItemStack target;
        ItemStack source;
        if (second.getType() == Material.ENCHANTED_BOOK) {
            target = first;
            source = second;
        } else if (first.getType() == Material.ENCHANTED_BOOK && second.getType() != Material.ENCHANTED_BOOK) {
            target = second;
            source = first;
        } else if (first.getType() == second.getType()) {
            target = first;
            source = second;
        } else {
            return null;
        }

        ItemStack result = currentResult != null && !currentResult.getType().isAir()
                && currentResult.getType() == target.getType()
                ? currentResult.clone()
                : target.clone();
        return new AnvilMergeInput(target, source, result);
    }

    private boolean updateSingleBookDisplay(Player player,
                                            ItemStack result,
                                            Map<String, Integer> enchantments,
                                            EnchantmentManager enchantManager) {
        if (result == null || result.getType() != Material.ENCHANTED_BOOK || enchantments.size() != 1) {
            return false;
        }
        if (plugin.getCustomItemManager() == null || plugin.getCustomItemManager().getStellarisCodex() == null) {
            return false;
        }
        Map.Entry<String, Integer> entry = enchantments.entrySet().iterator().next();
        EnchantmentData data = enchantManager.getEnchantment(entry.getKey());
        if (data == null) {
            return false;
        }
        plugin.getCustomItemManager().getStellarisCodex()
                .updateEnchantedBookDisplay(player, result, data, entry.getValue());
        return true;
    }

    private EnchantmentData pickWeighted(List<EnchantmentData> list,
                                         Set<String> selectedIds,
                                         EnchantmentManager enchantManager,
                                         PDCManager pdc,
                                         ItemStack item) {
        int total = 0;
        for (EnchantmentData data : list) {
            if (canAddCandidate(data, selectedIds, enchantManager, pdc, item)) {
                total += Math.max(0, data.getObtain().getEnchantingTableWeight());
            }
        }
        if (total <= 0) {
            return null;
        }

        int roll = ThreadLocalRandom.current().nextInt(total);
        int cumulative = 0;
        for (EnchantmentData data : list) {
            if (!canAddCandidate(data, selectedIds, enchantManager, pdc, item)) {
                continue;
            }
            cumulative += Math.max(0, data.getObtain().getEnchantingTableWeight());
            if (roll < cumulative) {
                return data;
            }
        }
        return null;
    }

    private boolean canAddCandidate(EnchantmentData data,
                                    Set<String> selectedIds,
                                    EnchantmentManager enchantManager,
                                    PDCManager pdc,
                                    ItemStack item) {
        if (selectedIds.contains(data.getId())) {
            return false;
        }
        if (pdc.hasConflict(item, data, enchantManager::getEnchantment)) {
            return false;
        }
        return !EnchantmentConflictPolicy.hasCustomConflict(
                data.getId(),
                data,
                selectedIds,
                enchantManager::getEnchantment);
    }

    private Set<String> selectedCustomIds(Map<Enchantment, Integer> toAdd,
                                          Map<String, Integer> existing) {
        Set<String> selected = new HashSet<>(existing.keySet());
        for (Enchantment enchantment : toAdd.keySet()) {
            if (enchantment == null || enchantment.getKey() == null) {
                continue;
            }
            NamespacedKey key = enchantment.getKey();
            if (EnchantmentRegistry.getNamespace().equals(key.getNamespace())) {
                selected.add(key.getKey().toLowerCase(Locale.ROOT));
            }
        }
        return selected;
    }

    private Enchantment resolveTrueEnchantment(String enchantId) {
        if (enchantId == null || enchantId.isBlank()) {
            return null;
        }
        return Registry.ENCHANTMENT.get(new NamespacedKey(
                EnchantmentRegistry.getNamespace(),
                enchantId.toLowerCase(Locale.ROOT)));
    }

    private static boolean isEnchantingTableBook(ItemStack item) {
        return item != null && item.getType() == Material.BOOK;
    }

    private record PendingCustomEnchant(String id, int level) {
    }

    private record AnvilMergeInput(ItemStack target, ItemStack source, ItemStack result) {
    }
}
