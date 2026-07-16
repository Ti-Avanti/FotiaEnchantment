package gg.fotia.enchantment.core;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.compat.BukkitItemFlags;
import gg.fotia.enchantment.config.VanillaConfig;
import gg.fotia.enchantment.config.VanillaConfig.VanillaOverride;
import gg.fotia.enchantment.lore.item.EnchantmentDisplayPolicy;
import gg.fotia.enchantment.lore.item.EnchantmentLoreCleaner;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 原版附魔覆盖管理器
 * <p>通过事件监听方式拦截并修改原版附魔行为（Paper 不允许直接修改 Registry 属性）。
 * <ul>
 *     <li>EnchantItemEvent: 移除禁用附魔、限制最大等级</li>
 *     <li>PrepareAnvilEvent: 检查冲突和最大等级</li>
 * </ul>
 */
public class VanillaManager implements Listener {

    private static final long ENCHANTING_FAILURE_MESSAGE_COOLDOWN_MILLIS = 5000L;

    private final FotiaEnchantment plugin;
    private final VanillaConfig vanillaConfig;
    private final Map<UUID, PreparedOffer[]> preparedOffers = new ConcurrentHashMap<>();
    private final Map<UUID, EnchantingFailureMessage> lastEnchantingFailureMessages = new ConcurrentHashMap<>();

    public VanillaManager(FotiaEnchantment plugin) {
        this.plugin = plugin;
        this.vanillaConfig = new VanillaConfig(plugin);
    }

    /**
     * 初始化：加载配置并注册事件监听
     */
    public void init() {
        vanillaConfig.loadAll();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("原版附魔覆盖系统已启用，共覆盖 " + vanillaConfig.getAllOverrides().size() + " 个原版附魔");
    }

    /**
     * 关闭：注销事件监听
     */
    public void shutdown() {
        HandlerList.unregisterAll(this);
        preparedOffers.clear();
        lastEnchantingFailureMessages.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        preparedOffers.remove(playerId);
        lastEnchantingFailureMessages.remove(playerId);
    }

    /**
     * 重载配置并重新应用
     */
    public void reload() {
        vanillaConfig.reload();
        plugin.getLogger().info("原版附魔覆盖系统已重载，共覆盖 " + vanillaConfig.getAllOverrides().size() + " 个原版附魔");
    }

    /**
     * 检查原版附魔是否被禁用
     */
    public boolean isDisabled(Enchantment enchant) {
        VanillaOverride override = getOverrideFor(enchant);
        return override != null && override.isDisabled();
    }

    /**
     * 获取覆盖后的最大等级（-1 表示使用原版默认值）
     */
    public int getMaxLevel(Enchantment enchant) {
        VanillaOverride override = getOverrideFor(enchant);
        if (override == null || override.getMaxLevel() == -1) {
            return enchant.getMaxLevel();
        }
        return override.getMaxLevel();
    }

    /**
     * 获取附魔台可生成的最大等级。
     * <p>默认使用原版附魔台等级上限，不会因为 max-level 提高而自动让附魔台生成超原版等级。</p>
     */
    private int getEnchantingTableMaxLevel(Enchantment enchant) {
        int generalMaxLevel = getMaxLevel(enchant);
        VanillaOverride override = getOverrideFor(enchant);
        int configuredTableMax = override != null ? override.getEnchantingTableMaxLevel() : -1;
        int tableMaxLevel = configuredTableMax == -1 ? enchant.getMaxLevel() : configuredTableMax;
        return Math.min(generalMaxLevel, tableMaxLevel);
    }

    /**
     * 获取冲突列表（包括原版冲突 + 额外配置冲突）
     */
    public List<String> getConflicts(Enchantment enchant) {
        VanillaOverride override = getOverrideFor(enchant);
        if (override == null) {
            return Collections.emptyList();
        }
        return override.getConflicts();
    }

    /**
     * 检查附魔是否适用于指定物品
     */
    public boolean isApplicable(Enchantment enchant, ItemStack item) {
        VanillaOverride override = getOverrideFor(enchant);
        if (override == null || override.getApplicableItems().isEmpty()) {
            // 无覆盖或未配置适用物品，使用原版逻辑
            return enchant.canEnchantItem(item);
        }
        // 使用配置的适用物品类型
        for (String applicable : override.getApplicableItems()) {
            if (matchesApplicableItemToken(item.getType(), applicable)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEnchantingTableBook(ItemStack item) {
        return item != null && item.getType() == Material.BOOK;
    }

    static boolean matchesApplicableItemToken(Material itemType, String configuredToken) {
        if (itemType == null || configuredToken == null) {
            return false;
        }

        String token = configuredToken.trim().toUpperCase(Locale.ROOT);
        if (token.isEmpty()) {
            return false;
        }

        String itemName = itemType.name();
        if (itemName.equals(token)) {
            return true;
        }

        return switch (token) {
            case "SWORD" -> itemName.endsWith("_SWORD");
            case "AXE" -> itemName.endsWith("_AXE");
            case "SPEAR" -> itemName.endsWith("_SPEAR");
            case "PICKAXE" -> itemName.endsWith("_PICKAXE");
            case "SHOVEL" -> itemName.endsWith("_SHOVEL");
            case "HOE" -> itemName.endsWith("_HOE");
            case "HELMET" -> itemName.endsWith("_HELMET");
            case "CHESTPLATE" -> itemName.endsWith("_CHESTPLATE");
            case "LEGGINGS" -> itemName.endsWith("_LEGGINGS");
            case "BOOTS" -> itemName.endsWith("_BOOTS");
            default -> false;
        };
    }

    /**
     * 获取附魔台权重（-1 表示使用原版默认值）
     */
    public int getWeight(Enchantment enchant) {
        VanillaOverride override = getOverrideFor(enchant);
        if (override == null || override.getEnchantingTableWeight() == -1) {
            return -1;
        }
        return override.getEnchantingTableWeight();
    }

    /**
     * 获取 VanillaConfig 实例
     */
    public VanillaConfig getVanillaConfig() {
        return vanillaConfig;
    }

    // ==================== 事件监听 ====================

    /**
     * 监听附魔台预览事件
     * - 隐藏被禁用或不适用的原版附魔
     * - 在配置权重时按权重重选预览候选
     * - 限制预览等级不超过配置最大等级
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        if (hasExistingFotiaEnchantments(item)) {
            clearEnchantingOffers(event.getOffers());
            event.setCancelled(true);
            return;
        }

        boolean useConfiguredWeights = hasConfiguredEnchantingWeights();
        EnchantmentOffer[] offers = event.getOffers();
        int enchantingSeed = event.getEnchanter().getEnchantmentSeed();
        for (int slot = 0; slot < offers.length; slot++) {
            EnchantmentOffer offer = offers[slot];
            if (offer == null || offer.getEnchantment() == null) {
                continue;
            }

            Enchantment current = offer.getEnchantment();
            boolean invalidOffer = isDisabled(current)
                    || !canApplyEnchantingTableOffer(item, current);
            if (invalidOffer || useConfiguredWeights) {
                CandidateOffer replacement = pickEnchantingTableCandidate(
                        item,
                        useConfiguredWeights,
                        enchantingSeed,
                        slot,
                        offer.getCost(),
                        event.getEnchantmentBonus(),
                        current,
                        offer.getEnchantmentLevel());
                if (replacement != null) {
                    applyOfferEnchantment(offer, replacement);
                    continue;
                }
                offers[slot] = null;
                continue;
            }

            if (!invalidOffer) {
                clampOfferLevel(offer, current);
            }
        }
        cachePreparedOffers(event.getEnchanter().getUniqueId(), item.getType(), offers);
    }

    /**
     * 监听附魔台附魔事件
     * - 移除被禁用的附魔
     * - 限制附魔等级不超过配置的最大等级
     * - 检查适用物品
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        Map<Enchantment, Integer> toAdd = event.getEnchantsToAdd();
        ItemStack item = event.getItem();
        PreparedOffer selected = consumePreparedOffer(event, item);
        if (hasExistingFotiaEnchantments(item) && selected == null) {
            toAdd.clear();
            event.setCancelled(true);
            notifyEnchantingTableFailure(event.getEnchanter(), "enchanting-table-already-fotia");
            return;
        }

        Enchantment preferred = syncWithPreparedOffer(event, toAdd, item, selected);

        Iterator<Map.Entry<Enchantment, Integer>> iterator = toAdd.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Enchantment, Integer> entry = iterator.next();
            Enchantment enchant = entry.getKey();

            // 移除被禁用的附魔
            if (isDisabled(enchant)) {
                iterator.remove();
                continue;
            }

            // 检查适用物品
            if (!isEnchantingTableBook(item) && !isApplicable(enchant, item)) {
                iterator.remove();
                continue;
            }

            // 限制最大等级
            int maxLevel = getEnchantingTableMaxLevel(enchant);
            if (entry.getValue() > maxLevel) {
                entry.setValue(maxLevel);
            }
        }

        if (item != null && plugin.getConfigManager() != null && plugin.getEnchantmentManager() != null) {
            removeConflictingEnchantingTableAdds(item, toAdd, preferred);
            trimEnchantingTableAddsToLimit(item, toAdd, preferred);
        }
        if (toAdd.isEmpty()) {
            preferred = recoverEnchantingTableResult(event, item, toAdd);
            if (!toAdd.isEmpty() && item != null
                    && plugin.getConfigManager() != null
                    && plugin.getEnchantmentManager() != null) {
                removeConflictingEnchantingTableAdds(item, toAdd, preferred);
                trimEnchantingTableAddsToLimit(item, toAdd, preferred);
            }
        }
        if (toAdd.isEmpty()) {
            event.setCancelled(true);
            notifyEnchantingTableFailure(event.getEnchanter(), "enchanting-table-no-result");
        }
    }

    /**
     * 监听铁砧准备事件
     * - 移除被禁用的附魔
     * - 检查冲突附魔
     * - 限制最大等级
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        boolean fallbackResult = false;
        if (result == null || result.getType() == Material.AIR) {
            result = fallbackReversedAnvilResult(event);
            fallbackResult = result != null && !result.getType().isAir();
            if (result == null || result.getType() == Material.AIR) {
                return;
            }
        }
        ItemStack displaySource = result.clone();

        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return;
        }

        boolean modified = false;

        // 处理附魔书
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            Map<Enchantment, Integer> stored = new HashMap<>(storageMeta.getStoredEnchants());
            for (Map.Entry<Enchantment, Integer> entry : stored.entrySet()) {
                Enchantment enchant = entry.getKey();

                // 移除被禁用的附魔
                if (isDisabled(enchant)) {
                    storageMeta.removeStoredEnchant(enchant);
                    modified = true;
                    continue;
                }

                // 限制最大等级
                int maxLevel = getMaxLevel(enchant);
                if (entry.getValue() > maxLevel) {
                    storageMeta.removeStoredEnchant(enchant);
                    storageMeta.addStoredEnchant(enchant, maxLevel, true);
                    modified = true;
                }

                // 检查冲突
                if (hasConflictWith(enchant, stored)) {
                    storageMeta.removeStoredEnchant(enchant);
                    modified = true;
                }
            }
        } else {
            modified |= mergeAnvilInputVanillaEnchantments(event, result, meta);

            // 处理普通物品上的附魔
            Map<Enchantment, Integer> enchants = new HashMap<>(meta.getEnchants());
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                Enchantment enchant = entry.getKey();

                // 移除被禁用的附魔
                if (isDisabled(enchant)) {
                    meta.removeEnchant(enchant);
                    modified = true;
                    continue;
                }

                // 限制最大等级
                int maxLevel = getMaxLevel(enchant);
                if (entry.getValue() > maxLevel) {
                    meta.removeEnchant(enchant);
                    meta.addEnchant(enchant, maxLevel, true);
                    modified = true;
                }

                // 检查适用物品
                if (!isApplicable(enchant, result)) {
                    meta.removeEnchant(enchant);
                    modified = true;
                }

                // 检查冲突
                if (meta.hasEnchant(enchant) && hasConflictWith(enchant, meta.getEnchants())) {
                    meta.removeEnchant(enchant);
                    modified = true;
                }
            }
        }

        if (modified) {
            result.setItemMeta(meta);
        }
        if (!modified && fallbackResult) {
            return;
        }
        if (isAnvilResultOverLimit(event, result)) {
            event.setResult(null);
            return;
        }
        modified |= applyAnvilResultDisplay(event, result, displaySource);
        if (modified) {
            event.setResult(result);
        }
    }

    // ==================== 内部工具方法 ====================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        ItemStack result = event.getResult();
        if (result == null || result.getType() == Material.AIR) {
            return;
        }

        ItemStack source = firstGrindstoneInput(event);
        ItemStack displayResult = result.clone();
        boolean changed = removeCustomEnchantments(displayResult);
        HumanEntity viewer = event.getView().getPlayer();
        changed |= applyResultDisplay(viewer, displayResult, source);
        if (changed) {
            event.setResult(displayResult);
        }
    }

    public boolean hasDisabledEnchantments(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && hasDisabledEnchantments(meta);
    }

    public boolean removeDisabledEnchantments(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        boolean changed = removeDisabledEnchantments(meta);
        if (changed) {
            item.setItemMeta(meta);
        }
        return changed;
    }

    private boolean hasDisabledEnchantments(ItemMeta meta) {
        for (Enchantment enchantment : meta.getEnchants().keySet()) {
            if (isMinecraftEnchantment(enchantment) && isDisabled(enchantment)) {
                return true;
            }
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Enchantment enchantment : storageMeta.getStoredEnchants().keySet()) {
                if (isMinecraftEnchantment(enchantment) && isDisabled(enchantment)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean removeDisabledEnchantments(ItemMeta meta) {
        boolean changed = false;
        for (Enchantment enchantment : new ArrayList<>(meta.getEnchants().keySet())) {
            if (isMinecraftEnchantment(enchantment) && isDisabled(enchantment)) {
                meta.removeEnchant(enchantment);
                changed = true;
            }
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Enchantment enchantment : new ArrayList<>(storageMeta.getStoredEnchants().keySet())) {
                if (isMinecraftEnchantment(enchantment) && isDisabled(enchantment)) {
                    storageMeta.removeStoredEnchant(enchantment);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private boolean removeCustomEnchantments(ItemStack item) {
        if (item == null || item.getType().isAir() || plugin.getEnchantmentManager() == null) {
            return false;
        }
        PDCManager pdc = plugin.getEnchantmentManager().getPdcManager();
        Map<String, Integer> customEnchantments = new HashMap<>(pdc.getEnchantments(item));
        if (customEnchantments.isEmpty()) {
            return false;
        }
        for (String enchantId : customEnchantments.keySet()) {
            pdc.removeEnchantment(item, enchantId);
        }
        return true;
    }

    private ItemStack firstGrindstoneInput(PrepareGrindstoneEvent event) {
        ItemStack first = event.getInventory().getItem(0);
        if (first != null && !first.getType().isAir()) {
            return first;
        }
        ItemStack second = event.getInventory().getItem(1);
        if (second != null && !second.getType().isAir()) {
            return second;
        }
        return null;
    }

    private ItemStack fallbackReversedAnvilResult(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getItem(0);
        ItemStack second = event.getInventory().getItem(1);
        if (first == null || second == null || second.getType().isAir()
                || first.getType() != Material.ENCHANTED_BOOK
                || second.getType() == Material.ENCHANTED_BOOK) {
            return null;
        }
        return second.clone();
    }

    private void removeConflictingEnchantingTableAdds(ItemStack item,
                                                      Map<Enchantment, Integer> toAdd,
                                                      Enchantment preferred) {
        if (item == null || toAdd == null || toAdd.isEmpty() || plugin.getEnchantmentManager() == null) {
            return;
        }

        Map<Enchantment, Integer> keptNative = nativeEnchantments(item);
        Set<String> keptCustomIds = customEnchantIds(item);
        List<Map.Entry<Enchantment, Integer>> ordered = orderedPendingAdds(toAdd, preferred);
        Set<Enchantment> allowed = new HashSet<>();

        for (Map.Entry<Enchantment, Integer> entry : ordered) {
            Enchantment candidate = entry.getKey();
            Integer level = entry.getValue();
            if (candidate == null || level == null || level <= 0) {
                continue;
            }
            if (candidateConflictsWithKept(candidate, keptNative, keptCustomIds)) {
                continue;
            }

            allowed.add(candidate);
            keptNative.put(candidate, level);
            String customId = customEnchantmentId(candidate);
            if (customId != null) {
                keptCustomIds.add(customId);
            }
        }

        toAdd.keySet().removeIf(enchantment -> !allowed.contains(enchantment));
    }

    private void trimEnchantingTableAddsToLimit(ItemStack item,
                                                Map<Enchantment, Integer> toAdd,
                                                Enchantment preferred) {
        if (item == null || toAdd == null || toAdd.isEmpty()
                || plugin.getConfigManager() == null
                || plugin.getEnchantmentManager() == null) {
            return;
        }

        int max = plugin.getConfigManager().getMaxEnchantmentsForMaterial(item.getType());
        if (max < 0) {
            return;
        }

        PDCManager pdc = plugin.getEnchantmentManager().getPdcManager();
        Set<String> keys = limitKeys(item, pdc);
        int count = keys.size();
        List<Map.Entry<Enchantment, Integer>> ordered = orderedPendingAdds(toAdd, preferred);
        Set<Enchantment> allowed = new HashSet<>();

        for (Map.Entry<Enchantment, Integer> entry : ordered) {
            Enchantment enchantment = entry.getKey();
            Integer level = entry.getValue();
            if (enchantment == null || level == null || level <= 0) {
                continue;
            }
            String key = limitKey(enchantment);
            if (keys.contains(key)) {
                allowed.add(enchantment);
                continue;
            }
            if (count >= max) {
                continue;
            }
            keys.add(key);
            count++;
            allowed.add(enchantment);
        }

        toAdd.keySet().removeIf(enchantment -> !allowed.contains(enchantment));
    }

    private boolean canApplyEnchantingTableOffer(ItemStack item, Enchantment enchantment) {
        if (item == null || enchantment == null) {
            return false;
        }
        if (isFotiaEnchantment(enchantment)) {
            return customEnchantingTableCandidateData(enchantment, item) != null
                    && canFitEnchantingTableOfferLimit(item, enchantment);
        }
        if (isDisabled(enchantment)
                || (!isEnchantingTableBook(item) && !isApplicable(enchantment, item))) {
            return false;
        }

        Map<Enchantment, Integer> keptNative = nativeEnchantments(item);
        for (Enchantment existing : keptNative.keySet()) {
            if (nativeEnchantmentConflict(enchantment, existing)) {
                return false;
            }
        }

        if (plugin.getEnchantmentManager() != null
                && candidateConflictsWithKept(enchantment, keptNative, customEnchantIds(item))) {
            return false;
        }

        return canFitEnchantingTableOfferLimit(item, enchantment);
    }

    private boolean canFitEnchantingTableOfferLimit(ItemStack item, Enchantment enchantment) {
        if (plugin.getConfigManager() == null || plugin.getEnchantmentManager() == null) {
            return true;
        }

        int max = plugin.getConfigManager().getMaxEnchantmentsForMaterial(item.getType());
        if (max < 0) {
            return true;
        }

        PDCManager pdc = plugin.getEnchantmentManager().getPdcManager();
        Set<String> keys = limitKeys(item, pdc);
        String key = limitKey(enchantment);
        return keys.contains(key) || keys.size() < max;
    }

    private List<Map.Entry<Enchantment, Integer>> orderedPendingAdds(Map<Enchantment, Integer> toAdd,
                                                                     Enchantment preferred) {
        List<Map.Entry<Enchantment, Integer>> ordered = new ArrayList<>(toAdd.entrySet());
        if (preferred != null) {
            ordered.sort((first, second) -> {
                boolean firstPreferred = preferred.equals(first.getKey());
                boolean secondPreferred = preferred.equals(second.getKey());
                return Boolean.compare(secondPreferred, firstPreferred);
            });
        }
        return ordered;
    }

    private boolean candidateConflictsWithKept(Enchantment candidate,
                                               Map<Enchantment, Integer> keptNative,
                                               Set<String> keptCustomIds) {
        for (Enchantment existing : keptNative.keySet()) {
            if (nativeEnchantmentConflict(candidate, existing)) {
                return true;
            }
        }

        EnchantmentManager manager = plugin.getEnchantmentManager();
        String customId = customEnchantmentId(candidate);
        if (customId != null) {
            EnchantmentData data = manager.getEnchantment(customId);
            if (data != null
                    && EnchantmentConflictPolicy.hasCustomConflict(
                    customId,
                    data,
                    keptCustomIds,
                    manager::getEnchantment)) {
                return true;
            }
            if (data != null && conflictsWithAnyNative(data, keptNative.keySet())) {
                return true;
            }
        } else {
            for (String existingCustomId : keptCustomIds) {
                EnchantmentData existingData = manager.getEnchantment(existingCustomId);
                if (existingData != null
                        && EnchantmentConflictPolicy.referencesBukkit(existingData.getConflicts(), candidate)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean conflictsWithAnyNative(EnchantmentData data, Set<Enchantment> nativeEnchantments) {
        for (Enchantment nativeEnchantment : nativeEnchantments) {
            if (EnchantmentConflictPolicy.referencesBukkit(data.getConflicts(), nativeEnchantment)) {
                return true;
            }
        }
        return false;
    }

    private Map<Enchantment, Integer> nativeEnchantments(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return new HashMap<>();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return new HashMap<>();
        }
        Map<Enchantment, Integer> result = new HashMap<>(meta.getEnchants());
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            result.putAll(storageMeta.getStoredEnchants());
        }
        return result;
    }

    private Set<String> customEnchantIds(ItemStack item) {
        if (plugin.getEnchantmentManager() == null) {
            return new HashSet<>();
        }
        PDCManager pdc = plugin.getEnchantmentManager().getPdcManager();
        Set<String> result = new HashSet<>();
        for (String id : pdc.getEnchantments(item).keySet()) {
            result.add(EnchantmentConflictPolicy.normalizeCustomId(id));
        }
        return result;
    }

    private Set<String> limitKeys(ItemStack item, PDCManager pdc) {
        Set<String> keys = new HashSet<>();
        for (Enchantment enchantment : nativeEnchantments(item).keySet()) {
            keys.add(limitKey(enchantment));
        }
        if (pdc != null) {
            for (String id : pdc.getLegacyEnchantments(item).keySet()) {
                keys.add(limitKey(id));
            }
        }
        return keys;
    }

    private String limitKey(Enchantment enchantment) {
        NamespacedKey key = enchantment.getKey();
        return key == null ? enchantment.toString().toLowerCase(Locale.ROOT) : key.toString().toLowerCase(Locale.ROOT);
    }

    private String limitKey(String customId) {
        String normalized = EnchantmentConflictPolicy.normalizeCustomId(customId);
        return normalized.contains(":")
                ? normalized
                : EnchantmentRegistry.getNamespace() + ":" + normalized;
    }

    private String customEnchantmentId(Enchantment enchantment) {
        if (enchantment == null || enchantment.getKey() == null) {
            return null;
        }
        NamespacedKey key = enchantment.getKey();
        if (!EnchantmentRegistry.getNamespace().equals(key.getNamespace())) {
            return null;
        }
        return key.getKey().toLowerCase(Locale.ROOT);
    }

    private boolean mergeAnvilInputVanillaEnchantments(PrepareAnvilEvent event, ItemStack result, ItemMeta resultMeta) {
        ItemStack first = event.getInventory().getItem(0);
        ItemStack second = event.getInventory().getItem(1);
        ItemStack target = anvilMergeTarget(first, second);
        ItemStack source = anvilMergeSource(first, second);
        if (target == null || source == null || result.getType() != target.getType()) {
            return false;
        }

        Map<Enchantment, Integer> incoming = incomingVanillaEnchants(target, source);
        if (incoming.isEmpty()) {
            return false;
        }

        boolean modified = false;
        for (Map.Entry<Enchantment, Integer> entry : incoming.entrySet()) {
            Enchantment enchant = entry.getKey();
            int incomingLevel = entry.getValue() == null ? 0 : entry.getValue();
            if (enchant == null
                    || incomingLevel <= 0
                    || !isMinecraftEnchantment(enchant)
                    || isDisabled(enchant)
                    || !isApplicable(enchant, result)) {
                continue;
            }
            if (hasAnvilConflict(enchant, resultMeta.getEnchants())) {
                continue;
            }

            int resultLevel = resultMeta.getEnchantLevel(enchant);
            if (resultLevel <= 0 && !canAddAnvilResultEnchantment(result, resultMeta)) {
                continue;
            }

            int firstInputLevel = vanillaEnchantLevel(target, enchant);
            int mergedLevel = mergeAnvilInputLevel(firstInputLevel, resultLevel, incomingLevel, getMaxLevel(enchant));
            if (mergedLevel > resultLevel) {
                resultMeta.addEnchant(enchant, mergedLevel, true);
                modified = true;
            }
        }
        return modified;
    }

    private ItemStack anvilMergeTarget(ItemStack first, ItemStack second) {
        if (first == null || second == null || first.getType().isAir() || second.getType().isAir()) {
            return null;
        }
        if (first.getType() == Material.ENCHANTED_BOOK && second.getType() != Material.ENCHANTED_BOOK) {
            return second;
        }
        return first;
    }

    private ItemStack anvilMergeSource(ItemStack first, ItemStack second) {
        if (first == null || second == null || first.getType().isAir() || second.getType().isAir()) {
            return null;
        }
        if (first.getType() == Material.ENCHANTED_BOOK && second.getType() != Material.ENCHANTED_BOOK) {
            return first;
        }
        return second;
    }

    public boolean hasAnvilConflict(Enchantment enchant, Map<Enchantment, Integer> existingEnchants) {
        return hasConflictWith(enchant, existingEnchants);
    }

    private Map<Enchantment, Integer> incomingVanillaEnchants(ItemStack first, ItemStack second) {
        ItemMeta secondMeta = second.getItemMeta();
        if (secondMeta == null) {
            return Collections.emptyMap();
        }
        if (second.getType() == Material.ENCHANTED_BOOK && secondMeta instanceof EnchantmentStorageMeta storageMeta) {
            return storageMeta.getStoredEnchants();
        }
        if (second.getType() == first.getType()) {
            return secondMeta.getEnchants();
        }
        return Collections.emptyMap();
    }

    private int vanillaEnchantLevel(ItemStack item, Enchantment enchant) {
        ItemMeta meta = item.getItemMeta();
        return meta == null ? 0 : meta.getEnchantLevel(enchant);
    }

    private boolean canAddAnvilResultEnchantment(ItemStack result, ItemMeta resultMeta) {
        if (plugin.getConfigManager() == null || plugin.getEnchantmentManager() == null) {
            return true;
        }

        int max = plugin.getConfigManager().getMaxEnchantmentsForMaterial(result.getType());
        if (max < 0) {
            return true;
        }

        ItemStack probe = result.clone();
        probe.setItemMeta(resultMeta);
        PDCManager pdc = plugin.getEnchantmentManager().getPdcManager();
        return EnchantmentLimitPolicy.canAddNewEnchantment(EnchantmentLimitPolicy.countEnchantments(probe, pdc), max);
    }

    private boolean isAnvilResultOverLimit(PrepareAnvilEvent event, ItemStack result) {
        if (result == null || result.getType() == Material.AIR
                || plugin.getConfigManager() == null
                || plugin.getEnchantmentManager() == null) {
            return false;
        }

        int max = plugin.getConfigManager().getMaxEnchantmentsForMaterial(result.getType());
        if (max < 0) {
            return false;
        }

        PDCManager pdc = plugin.getEnchantmentManager().getPdcManager();
        int resultCount = EnchantmentLimitPolicy.countEnchantments(result, pdc);
        int firstCount = EnchantmentLimitPolicy.countEnchantments(event.getInventory().getItem(0), pdc);
        int secondCount = EnchantmentLimitPolicy.countEnchantments(event.getInventory().getItem(1), pdc);
        return EnchantmentLimitPolicy.isLimitWorsened(resultCount, firstCount, secondCount, max);
    }

    static int mergeAnvilLevel(int existingLevel, int incomingLevel, int maxLevel) {
        int cap = Math.max(1, maxLevel);
        if (existingLevel <= 0) {
            return Math.min(incomingLevel, cap);
        }
        if (existingLevel == incomingLevel) {
            return Math.min(existingLevel + 1, cap);
        }
        return Math.min(Math.max(existingLevel, incomingLevel), cap);
    }

    static int mergeAnvilInputLevel(int firstInputLevel, int resultLevel, int incomingLevel, int maxLevel) {
        int expectedLevel = mergeAnvilLevel(firstInputLevel, incomingLevel, maxLevel);
        return Math.max(Math.max(0, resultLevel), expectedLevel);
    }

    private boolean applyAnvilResultDisplay(PrepareAnvilEvent event, ItemStack result, ItemStack source) {
        HumanEntity viewer = event.getView().getPlayer();
        return applyResultDisplay(viewer, result, source);
    }

    private boolean applyResultDisplay(HumanEntity viewer, ItemStack result, ItemStack source) {
        Player player = viewer instanceof Player p ? p : null;
        boolean changed = source == null
                ? EnchantmentLoreCleaner.applyGeneratedLore(plugin, player, result)
                : EnchantmentLoreCleaner.applyGeneratedLoreFromSource(plugin, player, result, source);

        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return changed;
        }

        boolean hasStoredEnchants = meta instanceof EnchantmentStorageMeta storageMeta
                && !storageMeta.getStoredEnchants().isEmpty();
        PDCManager pdc = plugin.getEnchantmentManager() == null
                ? null
                : plugin.getEnchantmentManager().getPdcManager();
        boolean hasCustomEnchants = pdc != null && !pdc.getLegacyEnchantments(result).isEmpty();
        if (!EnchantmentDisplayPolicy.shouldHideNativeEnchantments(
                meta.hasEnchants(),
                hasStoredEnchants,
                hasCustomEnchants)) {
            return changed;
        }

        boolean flagsChanged = false;
        if (!BukkitItemFlags.hasHideEnchantments(meta)) {
            BukkitItemFlags.hideEnchantments(meta);
            flagsChanged = true;
        }
        if (!BukkitItemFlags.hasHideStoredEnchantments(meta)
                && BukkitItemFlags.addHideStoredEnchantments(meta)) {
            flagsChanged = true;
        }
        if (flagsChanged) {
            result.setItemMeta(meta);
            changed = true;
        }
        return changed;
    }

    /**
     * 根据 Enchantment 获取对应的覆盖配置
     */
    private VanillaOverride getOverrideFor(Enchantment enchant) {
        if (!isMinecraftEnchantment(enchant)) {
            return null;
        }
        String key = enchant.getKey().getKey().toLowerCase(Locale.ROOT);
        return vanillaConfig.getOverride(key);
    }

    /**
     * 是否启用了任意原版附魔台权重覆盖。
     */
    private boolean hasConfiguredEnchantingWeights() {
        for (VanillaOverride override : vanillaConfig.getAllOverrides().values()) {
            if (override.getEnchantingTableWeight() != -1) {
                return true;
            }
        }
        return false;
    }

    private boolean hasExistingFotiaEnchantments(ItemStack item) {
        if (item == null || item.getType().isAir() || plugin.getEnchantmentManager() == null) {
            return false;
        }
        PDCManager pdc = plugin.getEnchantmentManager().getPdcManager();
        return pdc != null && !pdc.getEnchantments(item).isEmpty();
    }

    private void clearEnchantingOffers(EnchantmentOffer[] offers) {
        if (offers == null) {
            return;
        }
        for (int i = 0; i < offers.length; i++) {
            offers[i] = null;
        }
    }

    /**
     * 按配置挑选一个可用于附魔台的原版候选。
     */
    private CandidateOffer pickEnchantingTableCandidate(ItemStack item,
                                                        boolean useConfiguredWeights,
                                                        int enchantingSeed,
                                                        int offerSlot,
                                                        int offerCost,
                                                        int enchantmentBonus,
                                                        Enchantment currentOffer,
                                                        int currentOfferLevel) {
        List<WeightedEnchantment> candidates = new ArrayList<>();
        int totalWeight = 0;

        for (Enchantment enchantment : Registry.ENCHANTMENT) {
            if (enchantment == null || enchantment.getKey() == null
                    || !isEnchantingTablePreviewCandidateNamespace(enchantment.getKey().getNamespace())) {
                continue;
            }
            if (!canApplyEnchantingTableOffer(item, enchantment)) {
                continue;
            }

            int weight;
            if (isMinecraftEnchantment(enchantment)) {
                if (isDisabled(enchantment)
                        || (!isEnchantingTableBook(item) && !isApplicable(enchantment, item))) {
                    continue;
                }
                weight = useConfiguredWeights ? configuredWeightOrDefault(enchantment) : 10;
            } else {
                EnchantmentData data = customEnchantingTableCandidateData(enchantment, item);
                if (data == null) {
                    continue;
                }
                weight = data.getObtain().getEnchantingTableWeight();
            }

            if (weight <= 0) {
                continue;
            }
            candidates.add(new WeightedEnchantment(enchantment, weight));
            totalWeight += weight;
        }
        candidates.sort(Comparator.comparing(candidate -> candidate.enchantment().getKey().asString()));

        if (candidates.isEmpty() || totalWeight <= 0) {
            return null;
        }

        int roll = stableEnchantingPreviewRoll(
                totalWeight,
                enchantingSeed,
                item.getType(),
                offerSlot,
                offerCost,
                enchantmentBonus,
                keyString(currentOffer),
                currentOfferLevel);
        int cursor = 0;
        for (WeightedEnchantment candidate : candidates) {
            cursor += candidate.weight();
            if (roll < cursor) {
                return createCandidateOffer(candidate.enchantment(), enchantingSeed, offerSlot, offerCost);
            }
        }
        return createCandidateOffer(candidates.get(candidates.size() - 1).enchantment(),
                enchantingSeed,
                offerSlot,
                offerCost);
    }

    private CandidateOffer createCandidateOffer(Enchantment enchantment,
                                                int enchantingSeed,
                                                int offerSlot,
                                                int offerCost) {
        int level = rollEnchantingTableLevel(enchantment, offerCost, enchantingSeed, offerSlot);
        return new CandidateOffer(enchantment, level);
    }

    static int stableEnchantingPreviewRoll(int totalWeight,
                                           int enchantingSeed,
                                           Material itemType,
                                           int offerSlot,
                                           int offerCost,
                                           int enchantmentBonus,
                                           String currentOfferKey,
                                           int currentOfferLevel) {
        if (totalWeight <= 0) {
            return 0;
        }
        long hash = 0xcbf29ce484222325L;
        hash = mix(hash, enchantingSeed);
        hash = mix(hash, itemType == null ? "" : itemType.name());
        hash = mix(hash, offerSlot);
        hash = mix(hash, offerCost);
        hash = mix(hash, enchantmentBonus);
        hash = mix(hash, currentOfferKey == null ? "" : currentOfferKey);
        hash = mix(hash, currentOfferLevel);
        return (int) Math.floorMod(finalMix(hash), (long) totalWeight);
    }

    private static long mix(long hash, int value) {
        return finalMix(hash ^ value);
    }

    private static long mix(long hash, String value) {
        long result = hash;
        for (int i = 0; i < value.length(); i++) {
            result = mix(result, value.charAt(i));
        }
        return result;
    }

    private static long finalMix(long value) {
        long result = value;
        result ^= result >>> 33;
        result *= 0xff51afd7ed558ccdL;
        result ^= result >>> 33;
        result *= 0xc4ceb9fe1a85ec53L;
        result ^= result >>> 33;
        return result;
    }

    private String keyString(Enchantment enchantment) {
        if (enchantment == null || enchantment.getKey() == null) {
            return "";
        }
        return enchantment.getKey().asString();
    }

    /**
     * 读取配置权重；未配置时使用基础权重 10。
     */
    private int configuredWeightOrDefault(Enchantment enchantment) {
        int configured = getWeight(enchantment);
        return configured >= 0 ? configured : 10;
    }

    private EnchantmentData customEnchantingTableCandidateData(Enchantment enchantment, ItemStack item) {
        if (!isFotiaEnchantment(enchantment)
                || plugin.getConfigManager() == null
                || plugin.getEnchantmentManager() == null
                || !plugin.getConfigManager().isEnchantingTableEnabled()) {
            return null;
        }

        String id = customEnchantmentId(enchantment);
        EnchantmentManager manager = plugin.getEnchantmentManager();
        EnchantmentData data = manager.getEnchantment(id);
        if (data == null
                || !data.isEnabled()
                || !data.getObtain().isEnchantingTable()
                || data.getObtain().getEnchantingTableWeight() <= 0) {
            return null;
        }

        PDCManager pdc = manager.getPdcManager();
        if (pdc == null) {
            return null;
        }
        if (!isEnchantingTableBook(item) && !pdc.isApplicable(item, data)) {
            return null;
        }
        if (pdc.hasConflict(item, data, manager::getEnchantment)) {
            return null;
        }
        return conflictsWithAnyNative(data, nativeEnchantments(item).keySet()) ? null : data;
    }

    /**
     * 把附魔台预览项替换为指定附魔并修正等级。
     */
    private void applyOfferEnchantment(EnchantmentOffer offer, CandidateOffer candidate) {
        offer.setEnchantment(candidate.enchantment());
        offer.setEnchantmentLevel(candidate.level());
        clampOfferLevel(offer, candidate.enchantment());
    }

    /**
     * 记录玩家当前看到的附魔台预览。实际附魔事件里的 toAdd 可能仍来自服务端原始随机结果，
     * 因此需要在 EnchantItemEvent 中按玩家点击的预览项进行一次对齐。
     */
    private void cachePreparedOffers(UUID playerId, Material itemType, EnchantmentOffer[] offers) {
        if (playerId == null || itemType == null || offers == null) {
            return;
        }
        PreparedOffer[] snapshot = new PreparedOffer[offers.length];
        for (int i = 0; i < offers.length; i++) {
            EnchantmentOffer offer = offers[i];
            if (offer != null && offer.getEnchantment() != null) {
                snapshot[i] = new PreparedOffer(
                        offer.getEnchantment(),
                        offer.getEnchantmentLevel(),
                        itemType);
            }
        }
        preparedOffers.put(playerId, snapshot);
    }

    /**
     * 将实际写入结果和附魔台预览保持一致。
     */
    private Enchantment syncWithPreparedOffer(EnchantItemEvent event,
                                              Map<Enchantment, Integer> toAdd,
                                              ItemStack item,
                                              PreparedOffer selected) {
        if (event == null || toAdd == null || item == null || item.getType() == Material.AIR) {
            return null;
        }
        if (selected == null) {
            selected = fallbackPreparedOffer(event, item);
        }
        if (selected == null || selected.enchantment() == null) {
            return null;
        }

        Enchantment enchantment = selected.enchantment();
        if (isDisabled(enchantment)
                || (!isEnchantingTableBook(item) && !isApplicable(enchantment, item))) {
            return null;
        }

        if (isMinecraftEnchantment(enchantment)) {
            toAdd.entrySet().removeIf(entry -> !shouldKeepDuringPreparedOfferSync(entry.getKey()));
        } else if (isFotiaEnchantment(enchantment)) {
            toAdd.entrySet().removeIf(entry -> isFotiaEnchantment(entry.getKey())
                    && !enchantment.equals(entry.getKey()));
        }

        int startLevel = Math.max(1, enchantment.getStartLevel());
        int level = Math.max(startLevel, Math.min(selected.level(), getEnchantingTableMaxLevel(enchantment)));
        toAdd.put(enchantment, level);
        return enchantment;
    }

    private Enchantment recoverEnchantingTableResult(EnchantItemEvent event,
                                                     ItemStack item,
                                                     Map<Enchantment, Integer> toAdd) {
        if (event == null || item == null || item.getType() == Material.AIR || toAdd == null) {
            return null;
        }
        CandidateOffer candidate = pickEnchantingTableCandidate(
                item,
                true,
                event.getEnchanter().getEnchantmentSeed(),
                event.whichButton(),
                event.getExpLevelCost(),
                0,
                event.getEnchantmentHint(),
                event.getLevelHint());
        if (candidate == null || candidate.enchantment() == null) {
            return null;
        }

        Enchantment enchantment = candidate.enchantment();
        int startLevel = Math.max(1, enchantment.getStartLevel());
        int level = Math.max(startLevel, Math.min(candidate.level(), getEnchantingTableMaxLevel(enchantment)));
        toAdd.put(enchantment, level);
        return enchantment;
    }

    private PreparedOffer consumePreparedOffer(EnchantItemEvent event, ItemStack item) {
        PreparedOffer[] offers = preparedOffers.remove(event.getEnchanter().getUniqueId());
        int button = event.whichButton();
        if (offers != null && button >= 0 && button < offers.length) {
            PreparedOffer offer = offers[button];
            if (offer != null && offer.itemType() == item.getType()) {
                return offer;
            }
        }
        return null;
    }

    private PreparedOffer fallbackPreparedOffer(EnchantItemEvent event, ItemStack item) {
        Enchantment hint = event.getEnchantmentHint();
        if (hint == null) {
            return null;
        }
        int level = rollEnchantingTableLevel(
                hint,
                event.getExpLevelCost(),
                event.getEnchanter().getEnchantmentSeed(),
                event.whichButton());
        return new PreparedOffer(hint, level, item.getType());
    }

    private boolean isMinecraftEnchantment(Enchantment enchantment) {
        return enchantment != null
                && enchantment.getKey() != null
                && "minecraft".equals(enchantment.getKey().getNamespace());
    }

    private boolean shouldKeepDuringPreparedOfferSync(Enchantment enchantment) {
        return enchantment != null
                && enchantment.getKey() != null
                && shouldKeepNamespaceDuringPreparedOfferSync(enchantment.getKey().getNamespace());
    }

    static boolean shouldKeepNamespaceDuringPreparedOfferSync(String namespace) {
        return "minecraft".equals(namespace) || EnchantmentRegistry.getNamespace().equals(namespace);
    }

    static boolean isEnchantingTablePreviewCandidateNamespace(String namespace) {
        return "minecraft".equals(namespace) || EnchantmentRegistry.getNamespace().equals(namespace);
    }

    private boolean isFotiaEnchantment(Enchantment enchantment) {
        return enchantment != null
                && enchantment.getKey() != null
                && EnchantmentRegistry.getNamespace().equals(enchantment.getKey().getNamespace());
    }

    private void notifyEnchantingTableFailure(Player player, String messageKey) {
        if (player != null && plugin.getMessageHelper() != null
                && !shouldThrottleEnchantingTableFailure(player, messageKey)) {
            plugin.getMessageHelper().sendMessage(player, messageKey);
        }
    }

    private boolean shouldThrottleEnchantingTableFailure(Player player, String messageKey) {
        if (player == null || messageKey == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        EnchantingFailureMessage previous = lastEnchantingFailureMessages.get(player.getUniqueId());
        if (previous != null
                && messageKey.equals(previous.messageKey())
                && now - previous.createdAtMillis() < ENCHANTING_FAILURE_MESSAGE_COOLDOWN_MILLIS) {
            return true;
        }
        lastEnchantingFailureMessages.put(player.getUniqueId(), new EnchantingFailureMessage(messageKey, now));
        return false;
    }

    /**
     * 限制附魔台预览等级在配置最大等级和原版起始等级之间。
     */
    private void clampOfferLevel(EnchantmentOffer offer, Enchantment enchantment) {
        int level = clampLevel(offer.getEnchantmentLevel(), enchantment);
        if (offer.getEnchantmentLevel() != level) {
            offer.setEnchantmentLevel(level);
        }
    }

    private int rollEnchantingTableLevel(Enchantment enchantment,
                                         int offerCost,
                                         int enchantingSeed,
                                         int offerSlot) {
        int maxLevel = Math.max(1, getEnchantingTableMaxLevel(enchantment));
        int level;
        if (plugin.getConfigManager() == null || !plugin.getConfigManager().isEnchantingTableLevelRollEnabled()) {
            level = EnchantingTableLevelPolicy.legacyScaledLevel(maxLevel, offerCost);
        } else {
            level = EnchantingTableLevelPolicy.rollLevel(
                    maxLevel,
                    offerCost,
                    enchantingSeed,
                    keyString(enchantment),
                    offerSlot,
                    plugin.getConfigManager().getEnchantingTableLevelTiers());
        }
        return clampLevel(level, enchantment);
    }

    private int clampLevel(int level, Enchantment enchantment) {
        int startLevel = Math.max(1, enchantment.getStartLevel());
        int maxLevel = Math.max(startLevel, getEnchantingTableMaxLevel(enchantment));
        return Math.max(startLevel, Math.min(level, maxLevel));
    }

    /**
     * 检查附魔是否与已有附魔存在冲突
     */
    private boolean hasConflictWith(Enchantment enchant, Map<Enchantment, Integer> existingEnchants) {
        if (enchant == null || existingEnchants == null || existingEnchants.isEmpty()) {
            return false;
        }

        for (Map.Entry<Enchantment, Integer> entry : existingEnchants.entrySet()) {
            Enchantment other = entry.getKey();
            if (nativeEnchantmentConflict(enchant, other)) {
                return true;
            }
        }
        return false;
    }

    private boolean nativeEnchantmentConflict(Enchantment first, Enchantment second) {
        if (first == null || second == null || first.equals(second)) {
            return false;
        }
        if (first.conflictsWith(second) || second.conflictsWith(first)) {
            return true;
        }
        return EnchantmentConflictPolicy.referencesBukkit(getConflicts(first), second)
                || EnchantmentConflictPolicy.referencesBukkit(getConflicts(second), first);
    }

    private record WeightedEnchantment(Enchantment enchantment, int weight) {
    }

    private record CandidateOffer(Enchantment enchantment, int level) {
    }

    private record PreparedOffer(Enchantment enchantment, int level, Material itemType) {
    }

    private record EnchantingFailureMessage(String messageKey, long createdAtMillis) {
    }
}
