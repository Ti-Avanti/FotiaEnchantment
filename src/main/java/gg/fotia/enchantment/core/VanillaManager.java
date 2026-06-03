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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private final FotiaEnchantment plugin;
    private final VanillaConfig vanillaConfig;
    private final Map<UUID, PreparedOffer[]> preparedOffers = new ConcurrentHashMap<>();

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

        boolean useConfiguredWeights = hasConfiguredEnchantingWeights();
        EnchantmentOffer[] offers = event.getOffers();
        int enchantingSeed = event.getEnchanter().getEnchantmentSeed();
        for (int slot = 0; slot < offers.length; slot++) {
            EnchantmentOffer offer = offers[slot];
            if (offer == null || offer.getEnchantment() == null) {
                continue;
            }

            Enchantment current = offer.getEnchantment();
            boolean invalidOffer = isDisabled(current) || !isApplicable(current, item);
            if (invalidOffer || useConfiguredWeights) {
                Enchantment replacement = pickEnchantingTableCandidate(
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

        Enchantment preferred = syncWithPreparedOffer(event, toAdd, item);

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
            VanillaOverride override = getOverrideFor(enchant);
            if (override != null && !override.getApplicableItems().isEmpty()) {
                if (!isApplicable(enchant, item)) {
                    iterator.remove();
                    continue;
                }
            }

            // 限制最大等级
            int maxLevel = getMaxLevel(enchant);
            if (entry.getValue() > maxLevel) {
                entry.setValue(maxLevel);
            }
        }

        if (item != null && plugin.getConfigManager() != null && plugin.getEnchantmentManager() != null) {
            int max = plugin.getConfigManager().getMaxEnchantmentsForMaterial(item.getType());
            PDCManager pdc = plugin.getEnchantmentManager().getPdcManager();
            int existingCount = EnchantmentLimitPolicy.countEnchantments(item, pdc);
            EnchantmentLimitPolicy.trimPendingEnchantmentsToLimit(toAdd, existingCount, max, preferred);
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
        if (result == null || result.getType() == Material.AIR) {
            return;
        }

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
                VanillaOverride override = getOverrideFor(enchant);
                if (override != null && !override.getApplicableItems().isEmpty()) {
                    if (!isApplicable(enchant, result)) {
                        meta.removeEnchant(enchant);
                        modified = true;
                    }
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
        modified |= applyAnvilResultDisplay(event, result);
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
        HumanEntity viewer = event.getView().getPlayer();
        if (applyResultDisplay(viewer, displayResult, source)) {
            event.setResult(displayResult);
        }
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

    private boolean mergeAnvilInputVanillaEnchantments(PrepareAnvilEvent event, ItemStack result, ItemMeta resultMeta) {
        ItemStack first = event.getInventory().getItem(0);
        ItemStack second = event.getInventory().getItem(1);
        if (first == null || second == null || result.getType() != first.getType()) {
            return false;
        }

        Map<Enchantment, Integer> incoming = incomingVanillaEnchants(first, second);
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

            int firstInputLevel = vanillaEnchantLevel(first, enchant);
            int mergedLevel = mergeAnvilInputLevel(firstInputLevel, resultLevel, incomingLevel, getMaxLevel(enchant));
            if (mergedLevel > resultLevel) {
                resultMeta.addEnchant(enchant, mergedLevel, true);
                modified = true;
            }
        }
        return modified;
    }

    private boolean hasAnvilConflict(Enchantment enchant, Map<Enchantment, Integer> existingEnchants) {
        if (hasConflictWith(enchant, existingEnchants)) {
            return true;
        }

        for (Enchantment other : existingEnchants.keySet()) {
            if (other.equals(enchant)) {
                continue;
            }
            String enchantKey = enchant.getKey().getKey().toLowerCase(Locale.ROOT);
            if (getConflicts(other).contains(enchantKey)) {
                return true;
            }
        }
        return false;
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

    private boolean applyAnvilResultDisplay(PrepareAnvilEvent event, ItemStack result) {
        HumanEntity viewer = event.getView().getPlayer();
        return applyResultDisplay(viewer, result, null);
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

    /**
     * 按配置挑选一个可用于附魔台的原版候选。
     */
    private Enchantment pickEnchantingTableCandidate(ItemStack item,
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
            if (!"minecraft".equals(enchantment.getKey().getNamespace())) {
                continue;
            }
            if (isDisabled(enchantment) || !isApplicable(enchantment, item)) {
                continue;
            }

            int weight = useConfiguredWeights ? configuredWeightOrDefault(enchantment) : 10;
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
                return candidate.enchantment();
            }
        }
        return candidates.get(candidates.size() - 1).enchantment();
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

    /**
     * 把附魔台预览项替换为指定附魔并修正等级。
     */
    private void applyOfferEnchantment(EnchantmentOffer offer, Enchantment enchantment) {
        offer.setEnchantment(enchantment);
        clampOfferLevel(offer, enchantment);
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
                                              ItemStack item) {
        if (event == null || toAdd == null || item == null || item.getType() == Material.AIR) {
            return null;
        }
        PreparedOffer selected = selectedPreparedOffer(event, item);
        if (selected == null || selected.enchantment() == null) {
            return null;
        }

        Enchantment enchantment = selected.enchantment();
        if (isDisabled(enchantment) || !isApplicable(enchantment, item)) {
            return null;
        }

        if (isMinecraftEnchantment(enchantment)) {
            toAdd.entrySet().removeIf(entry -> !shouldKeepDuringPreparedOfferSync(entry.getKey()));
        }

        int startLevel = Math.max(1, enchantment.getStartLevel());
        int level = Math.max(startLevel, Math.min(selected.level(), getMaxLevel(enchantment)));
        toAdd.put(enchantment, level);
        return enchantment;
    }

    private PreparedOffer selectedPreparedOffer(EnchantItemEvent event, ItemStack item) {
        PreparedOffer[] offers = preparedOffers.remove(event.getEnchanter().getUniqueId());
        int button = event.whichButton();
        if (offers != null && button >= 0 && button < offers.length) {
            PreparedOffer offer = offers[button];
            if (offer != null && offer.itemType() == item.getType()) {
                return offer;
            }
        }

        Enchantment hint = event.getEnchantmentHint();
        if (hint == null) {
            return null;
        }
        return new PreparedOffer(hint, event.getLevelHint(), item.getType());
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

    /**
     * 限制附魔台预览等级在配置最大等级和原版起始等级之间。
     */
    private void clampOfferLevel(EnchantmentOffer offer, Enchantment enchantment) {
        int startLevel = Math.max(1, enchantment.getStartLevel());
        int maxLevel = Math.max(startLevel, getMaxLevel(enchantment));
        int level = Math.max(startLevel, Math.min(offer.getEnchantmentLevel(), maxLevel));
        if (offer.getEnchantmentLevel() != level) {
            offer.setEnchantmentLevel(level);
        }
    }

    /**
     * 检查附魔是否与已有附魔存在冲突
     */
    private boolean hasConflictWith(Enchantment enchant, Map<Enchantment, Integer> existingEnchants) {
        List<String> conflicts = getConflicts(enchant);
        if (conflicts.isEmpty()) {
            return false;
        }

        for (Map.Entry<Enchantment, Integer> entry : existingEnchants.entrySet()) {
            Enchantment other = entry.getKey();
            if (other.equals(enchant)) {
                continue;
            }
            String otherKey = other.getKey().getKey().toLowerCase(Locale.ROOT);
            if (conflicts.contains(otherKey)) {
                return true;
            }
        }
        return false;
    }

    private record WeightedEnchantment(Enchantment enchantment, int weight) {
    }

    private record PreparedOffer(Enchantment enchantment, int level, Material itemType) {
    }
}
