package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.FotiaEnchantment;
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

        List<EnchantmentData> applicable = new ArrayList<>();
        for (EnchantmentData data : enchantManager.getEnabled()) {
            if (!data.getObtain().isEnchantingTable()) {
                continue;
            }
            if (!pdc.isApplicable(item, data)) {
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

            int level = Math.max(1, Math.min(picked.getMaxLevel(),
                    (int) Math.ceil(cost * picked.getMaxLevel() / 30.0)));
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

        if (second.getType() != Material.ENCHANTED_BOOK) {
            return;
        }
        Map<String, Integer> bookEnchants = pdc.getEnchantments(second);
        if (bookEnchants.isEmpty()) {
            return;
        }

        ItemStack result = event.getResult();
        if (result == null) {
            result = first.clone();
        } else {
            result = result.clone();
        }

        int max = plugin.getConfigManager().getMaxEnchantmentsForMaterial(result.getType());
        if (EnchantmentLimitPolicy.isLimitExceeded(EnchantmentLimitPolicy.countEnchantments(result, pdc), max)) {
            event.setResult(null);
            return;
        }
        ItemStack mergeTarget = result;
        Map<String, Integer> existing = pdc.getEnchantments(mergeTarget);
        AnvilCustomEnchantMerge.Result merge = AnvilCustomEnchantMerge.merge(
                existing,
                bookEnchants,
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
        if (pdc.hasConflict(item, data)) {
            return false;
        }
        return !conflictsWithSelected(data, selectedIds, enchantManager);
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

    private boolean conflictsWithSelected(EnchantmentData candidate,
                                          Set<String> selectedIds,
                                          EnchantmentManager enchantManager) {
        for (String selectedId : selectedIds) {
            if (candidate.getConflicts().contains(selectedId)) {
                return true;
            }
            EnchantmentData selected = enchantManager.getEnchantment(selectedId);
            if (selected != null && selected.getConflicts().contains(candidate.getId())) {
                return true;
            }
        }
        return false;
    }

    private Enchantment resolveTrueEnchantment(String enchantId) {
        if (enchantId == null || enchantId.isBlank()) {
            return null;
        }
        return Registry.ENCHANTMENT.get(new NamespacedKey(
                EnchantmentRegistry.getNamespace(),
                enchantId.toLowerCase(Locale.ROOT)));
    }

    private record PendingCustomEnchant(String id, int level) {
    }
}
