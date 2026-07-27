package gg.fotia.enchantment.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.fotia.enchantment.compat.BukkitItemFlags;
import gg.fotia.enchantment.compat.BukkitRegistryCompat;
import gg.fotia.enchantment.lore.item.EnchantmentDisplayPolicy;
import gg.fotia.enchantment.util.ItemUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * PDC管理器 - 通过 PersistentDataContainer 在物品上存储和读取自定义附魔数据。
 *
 * <p>存储格式: key=fotia:enchantments, value=JSON字符串 {"enchant_id":level, ...}
 *
 * <p>读取路径带解析缓存, 可被 Netty 数据包线程并发调用。
 */
public class PDCManager {

    private static final Gson GSON = new Gson();

    /** JSON字符串 → 解析结果缓存上限, 超过后整体清空防止无界增长 */
    private static final int PARSE_CACHE_MAX = 4096;

    /** 存储附魔数据的 NamespacedKey */
    private final NamespacedKey enchantmentsKey;

    private final Logger logger;

    /**
     * JSON字符串 → 不可变附魔Map 的解析缓存。
     * 相同附魔组合序列化出的 JSON 串完全一致, 命中率高; 值不可变, 供多线程安全共享。
     */
    private final Map<String, Map<String, Integer>> parseCache = new ConcurrentHashMap<>();

    /**
     * 附魔ID → 真附魔解析结果缓存。附魔注册表在服务器启动后冻结, 结果恒定。
     */
    private final Map<String, Optional<Enchantment>> trueEnchantmentCache = new ConcurrentHashMap<>();

    /** JSON 解析失败只告警一次, 避免损坏物品在背包巡检中反复刷屏 */
    private volatile boolean parseFailureLogged;

    public PDCManager(Plugin plugin) {
        this.enchantmentsKey = new NamespacedKey(plugin, "enchantments");
        this.logger = plugin.getLogger();
    }

    /**
     * 添加附魔到物品PDC
     *
     * @param item     目标物品
     * @param enchantId 附魔ID
     * @param level    附魔等级
     * @return 修改后的物品（可能是新实例）
     */
    public ItemStack addEnchantment(ItemStack item, String enchantId, int level) {
        if (item == null || enchantId == null || enchantId.isEmpty() || level < 1) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        Enchantment trueEnchantment = resolveTrueEnchantment(enchantId);
        if (trueEnchantment != null) {
            if (meta instanceof EnchantmentStorageMeta storageMeta) {
                storageMeta.addStoredEnchant(trueEnchantment, level, true);
            } else {
                meta.addEnchant(trueEnchantment, level, true);
            }

            String normalized = normalizeId(enchantId);
            Map<String, Integer> legacy = readEnchantments(meta);
            if (legacy.remove(normalized) != null) {
                writeEnchantments(meta, legacy);
            }
            hideNativeEnchantDisplay(meta, !legacy.isEmpty());
            item.setItemMeta(meta);
            return item;
        }

        Map<String, Integer> enchants = readEnchantments(meta);
        enchants.put(normalizeId(enchantId), level);
        writeEnchantments(meta, enchants);
        setLegacyCustomGlint(meta, true);
        hideNativeEnchantDisplay(meta, true);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 移除物品上的指定附魔
     *
     * @param item     目标物品
     * @param enchantId 附魔ID
     * @return 修改后的物品
     */
    public ItemStack removeEnchantment(ItemStack item, String enchantId) {
        if (item == null || enchantId == null || enchantId.isEmpty()) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        boolean modified = false;

        Enchantment trueEnchantment = resolveTrueEnchantment(enchantId);
        if (trueEnchantment != null) {
            if (meta instanceof EnchantmentStorageMeta storageMeta) {
                modified = storageMeta.removeStoredEnchant(trueEnchantment);
            } else {
                modified = meta.removeEnchant(trueEnchantment);
            }
        }

        Map<String, Integer> enchants = readEnchantments(meta);
        if (enchants.remove(normalizeId(enchantId)) != null) {
            if (enchants.isEmpty()) {
                meta.getPersistentDataContainer().remove(enchantmentsKey);
                setLegacyCustomGlint(meta, false);
            } else {
                writeEnchantments(meta, enchants);
                setLegacyCustomGlint(meta, true);
            }
            modified = true;
        }
        if (modified) {
            hideNativeEnchantDisplay(meta, !enchants.isEmpty());
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 批量覆写物品上的全部自定义附魔, 单次 meta 往返完成。
     *
     * <p>会先清除物品上现有的 fotia 命名空间真附魔与旧版 PDC 数据,
     * 再按传入映射逐个写入(已注册真附魔的走原生存储, 其余走 PDC JSON)。
     * 供物品有效性巡检等需要一次性修正多个附魔的调用方使用,
     * 避免逐条 remove/add 造成的重复 meta 克隆与 JSON 解析。
     *
     * @param item     目标物品
     * @param enchants 附魔ID→等级映射; null 或空表示清空全部自定义附魔
     * @return 修改后的物品
     */
    public ItemStack setEnchantments(ItemStack item, Map<String, Integer> enchants) {
        if (item == null) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        // 清除现有 fotia 命名空间真附魔
        for (Enchantment existing : meta.getEnchants().keySet().toArray(new Enchantment[0])) {
            if (isCustomTrueEnchantment(existing)) {
                meta.removeEnchant(existing);
            }
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Enchantment existing : storageMeta.getStoredEnchants().keySet().toArray(new Enchantment[0])) {
                if (isCustomTrueEnchantment(existing)) {
                    storageMeta.removeStoredEnchant(existing);
                }
            }
        }

        Map<String, Integer> legacy = new HashMap<>();
        if (enchants != null) {
            for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
                String id = entry.getKey();
                Integer level = entry.getValue();
                if (id == null || id.isEmpty() || level == null || level < 1) {
                    continue;
                }
                String normalized = normalizeId(id);
                Enchantment trueEnchantment = resolveTrueEnchantment(normalized);
                if (trueEnchantment != null) {
                    if (meta instanceof EnchantmentStorageMeta storageMeta) {
                        storageMeta.addStoredEnchant(trueEnchantment, level, true);
                    } else {
                        meta.addEnchant(trueEnchantment, level, true);
                    }
                } else {
                    legacy.put(normalized, level);
                }
            }
        }
        writeEnchantments(meta, legacy);
        setLegacyCustomGlint(meta, !legacy.isEmpty());
        hideNativeEnchantDisplay(meta, !legacy.isEmpty());
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 获取物品上所有自定义附魔
     *
     * @param item 目标物品
     * @return 附魔ID→等级的不可变映射，物品为空时返回空Map
     */
    public Map<String, Integer> getEnchantments(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Collections.emptyMap();
        }
        return getEnchantments(item.getItemMeta());
    }

    /**
     * 获取物品上所有自定义附魔 (供已持有 ItemMeta 的调用方使用, 避免重复的 meta 深拷贝)
     */
    public Map<String, Integer> getEnchantments(ItemMeta meta) {
        if (meta == null) {
            return Collections.emptyMap();
        }
        Map<String, Integer> legacy = readEnchantmentsShared(meta);
        Map<String, Integer> trueEnchants = readTrueEnchantments(meta);
        if (trueEnchants.isEmpty()) {
            return legacy;
        }
        if (legacy.isEmpty()) {
            return Collections.unmodifiableMap(trueEnchants);
        }
        Map<String, Integer> merged = new HashMap<>(legacy);
        merged.putAll(trueEnchants);
        return Collections.unmodifiableMap(merged);
    }

    /**
     * 只读取旧版 PDC 附魔数据。用于 PacketEvents 兼容 lore 与旧物品迁移。
     */
    public Map<String, Integer> getLegacyEnchantments(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Collections.emptyMap();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Collections.emptyMap();
        }
        return readEnchantmentsShared(meta);
    }

    /**
     * 只读取旧版 PDC 附魔数据 (供已持有 ItemMeta 的调用方使用)
     */
    public Map<String, Integer> getLegacyEnchantments(ItemMeta meta) {
        if (meta == null) {
            return Collections.emptyMap();
        }
        return readEnchantmentsShared(meta);
    }

    /**
     * 检查物品是否拥有指定附魔
     *
     * @param item     目标物品
     * @param enchantId 附魔ID
     * @return 是否拥有
     */
    public boolean hasEnchantment(ItemStack item, String enchantId) {
        if (item == null || enchantId == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Enchantment trueEnchantment = resolveTrueEnchantment(enchantId);
        if (trueEnchantment != null && hasTrueEnchantment(meta, trueEnchantment)) {
            return true;
        }
        return readEnchantmentsShared(meta).containsKey(normalizeId(enchantId));
    }

    /**
     * 获取物品上指定附魔的等级
     *
     * @param item     目标物品
     * @param enchantId 附魔ID
     * @return 附魔等级，不存在返回0
     */
    public int getEnchantmentLevel(ItemStack item, String enchantId) {
        if (item == null || enchantId == null || !item.hasItemMeta()) {
            return 0;
        }
        return getEnchantmentLevel(item.getItemMeta(), enchantId);
    }

    /**
     * 获取指定附魔等级 (供已持有 ItemMeta 的调用方使用, 避免重复的 meta 深拷贝)
     */
    public int getEnchantmentLevel(ItemMeta meta, String enchantId) {
        if (meta == null || enchantId == null) {
            return 0;
        }
        Enchantment trueEnchantment = resolveTrueEnchantment(enchantId);
        if (trueEnchantment != null) {
            int level = getTrueEnchantmentLevel(meta, trueEnchantment);
            if (level > 0) {
                return level;
            }
        }
        return readEnchantmentsShared(meta).getOrDefault(normalizeId(enchantId), 0);
    }

    public boolean isTrueEnchantmentRegistered(String enchantId) {
        return resolveTrueEnchantment(enchantId) != null;
    }

    /**
     * 检查物品是否适用该附魔
     *
     * @param item 目标物品
     * @param data 附魔数据
     * @return 是否适用
     */
    public boolean isApplicable(ItemStack item, EnchantmentData data) {
        if (item == null || data == null) {
            return false;
        }
        // 如果未配置适用物品列表，则默认适用所有
        if (data.getApplicableItems().isEmpty()) {
            return true;
        }
        return data.isApplicableTo(item.getType());
    }

    /**
     * 检查物品上是否存在与指定附魔冲突的附魔
     *
     * @param item 目标物品
     * @param data 要检查的附魔数据
     * @return 是否存在冲突
     */
    public boolean hasConflict(ItemStack item, EnchantmentData data) {
        return hasConflict(item, data, null);
    }

    public boolean hasConflict(ItemStack item,
                               EnchantmentData data,
                               Function<String, EnchantmentData> dataResolver) {
        if (item == null || data == null) {
            return false;
        }
        return hasConflict(item.hasItemMeta() ? item.getItemMeta() : null, data, dataResolver);
    }

    /**
     * 冲突检查 (供已持有 ItemMeta 的调用方使用, 避免重复的 meta 深拷贝)
     */
    public boolean hasConflict(ItemMeta meta,
                               EnchantmentData data,
                               Function<String, EnchantmentData> dataResolver) {
        if (data == null) {
            return false;
        }
        Map<String, Integer> existing = meta != null ? getEnchantments(meta) : Collections.emptyMap();
        return EnchantmentConflictPolicy.hasCustomConflict(data.getId(), data, existing, dataResolver)
                || (meta != null && findNativeConflict(meta, data) != null);
    }

    /**
     * 查找与 Fotia 附魔配置冲突的原版附魔，附魔书同时检查存储附魔。
     */
    public Enchantment findNativeConflict(ItemStack item, EnchantmentData data) {
        if (item == null || data == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return findNativeConflict(meta, data);
    }

    /**
     * 查找与 Fotia 附魔配置冲突的原版附魔 (ItemMeta 版本)
     */
    public Enchantment findNativeConflict(ItemMeta meta, EnchantmentData data) {
        if (meta == null || data == null) {
            return null;
        }
        Enchantment conflict = findNativeConflict(data, meta.getEnchants().keySet());
        if (conflict != null || !(meta instanceof EnchantmentStorageMeta storageMeta)) {
            return conflict;
        }
        return findNativeConflict(data, storageMeta.getStoredEnchants().keySet());
    }

    private Enchantment findNativeConflict(EnchantmentData data, Iterable<Enchantment> enchantments) {
        for (Enchantment enchantment : enchantments) {
            if (EnchantmentConflictPolicy.referencesBukkit(data, enchantment)) {
                return enchantment;
            }
        }
        return null;
    }

    /**
     * 获取存储附魔的 NamespacedKey
     */
    public NamespacedKey getEnchantmentsKey() {
        return enchantmentsKey;
    }

    // ==================== 内部方法 ====================

    /**
     * 从 ItemMeta 的 PDC 中读取附魔数据 (可变副本, 供需要修改后回写的调用方)
     */
    private Map<String, Integer> readEnchantments(ItemMeta meta) {
        return new HashMap<>(readEnchantmentsShared(meta));
    }

    /**
     * 从 ItemMeta 的 PDC 中读取附魔数据 (不可变共享实例, 走解析缓存, 禁止修改)
     */
    private Map<String, Integer> readEnchantmentsShared(ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String json = pdc.get(enchantmentsKey, PersistentDataType.STRING);
        if (json == null || json.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Integer> cached = parseCache.get(json);
        if (cached != null) {
            return cached;
        }
        Map<String, Integer> parsed = parseEnchantments(json);
        if (parseCache.size() >= PARSE_CACHE_MAX) {
            parseCache.clear();
        }
        parseCache.put(json, parsed);
        return parsed;
    }

    private Map<String, Integer> parseEnchantments(String json) {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) {
                logParseFailure(json, null);
                return Collections.emptyMap();
            }
            Map<String, Integer> enchants = new HashMap<>();
            JsonObject object = root.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                JsonElement value = entry.getValue();
                if (entry.getKey() == null || value == null || !value.isJsonPrimitive()) {
                    continue;
                }
                String id = normalizeStoredId(entry.getKey());
                int level = value.getAsInt();
                if (level > 0) {
                    enchants.merge(id, level, Math::max);
                }
            }
            return enchants.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(enchants);
        } catch (RuntimeException e) {
            logParseFailure(json, e);
            return Collections.emptyMap();
        }
    }

    private void logParseFailure(String json, RuntimeException e) {
        if (parseFailureLogged) {
            return;
        }
        parseFailureLogged = true;
        logger.warning("物品 PDC 附魔数据损坏, 无法解析 JSON (后续同类错误不再提示): "
                + json + (e != null ? " (" + e.getMessage() + ")" : ""));
    }

    /**
     * 将附魔数据写入 ItemMeta 的 PDC
     */
    private void writeEnchantments(ItemMeta meta, Map<String, Integer> enchants) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (enchants == null || enchants.isEmpty()) {
            pdc.remove(enchantmentsKey);
        } else {
            pdc.set(enchantmentsKey, PersistentDataType.STRING, GSON.toJson(enchants));
        }
    }

    private Map<String, Integer> readTrueEnchantments(ItemMeta meta) {
        boolean hasNative = meta.hasEnchants();
        boolean hasStored = hasStoredEnchantments(meta);
        if (!hasNative && !hasStored) {
            return Collections.emptyMap();
        }
        Map<String, Integer> result = new HashMap<>();
        if (hasNative) {
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                addCustomTrueEnchantment(result, entry.getKey(), entry.getValue());
            }
        }
        if (hasStored && meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<Enchantment, Integer> entry : storageMeta.getStoredEnchants().entrySet()) {
                addCustomTrueEnchantment(result, entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private void hideNativeEnchantDisplay(ItemMeta meta, boolean hasLegacyCustomEnchants) {
        if (EnchantmentDisplayPolicy.shouldHideNativeEnchantments(
                meta.hasEnchants(),
                hasStoredEnchantments(meta),
                hasLegacyCustomEnchants)) {
            BukkitItemFlags.hideEnchantments(meta);
        }
    }

    private void setLegacyCustomGlint(ItemMeta meta, boolean enabled) {
        if (enabled) {
            ItemUtils.applyPersistentCustomEnchantGlint(meta, true);
            return;
        }
        if (!meta.hasEnchants() && !hasStoredEnchantments(meta)) {
            ItemUtils.applyPersistentCustomEnchantGlint(meta, false);
        }
    }

    private boolean hasStoredEnchantments(ItemMeta meta) {
        return meta instanceof EnchantmentStorageMeta storageMeta
                && !storageMeta.getStoredEnchants().isEmpty();
    }

    private void addCustomTrueEnchantment(Map<String, Integer> result, Enchantment enchantment, int level) {
        if (enchantment == null || level <= 0) {
            return;
        }
        NamespacedKey key = enchantment.getKey();
        if (key != null && EnchantmentRegistry.getNamespace().equals(key.getNamespace())) {
            result.put(key.getKey(), level);
        }
    }

    private boolean isCustomTrueEnchantment(Enchantment enchantment) {
        if (enchantment == null) {
            return false;
        }
        NamespacedKey key = enchantment.getKey();
        return key != null && EnchantmentRegistry.getNamespace().equals(key.getNamespace());
    }

    private Enchantment resolveTrueEnchantment(String enchantId) {
        if (enchantId == null || enchantId.isBlank()) {
            return null;
        }
        // 附魔注册表启动后冻结, 解析结果恒定, 缓存避免每次的 NamespacedKey 分配与注册表查找
        return trueEnchantmentCache.computeIfAbsent(normalizeId(enchantId), id -> {
            try {
                return Optional.ofNullable(BukkitRegistryCompat.enchantment(new NamespacedKey(
                        EnchantmentRegistry.getNamespace(), id)));
            } catch (IllegalArgumentException e) {
                // id 含非法字符, 无法构成 NamespacedKey
                return Optional.empty();
            }
        }).orElse(null);
    }

    private boolean hasTrueEnchantment(ItemMeta meta, Enchantment enchantment) {
        return getTrueEnchantmentLevel(meta, enchantment) > 0;
    }

    private int getTrueEnchantmentLevel(ItemMeta meta, Enchantment enchantment) {
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            int level = storageMeta.getStoredEnchantLevel(enchantment);
            if (level > 0) {
                return level;
            }
        }
        return meta.getEnchantLevel(enchantment);
    }

    private String normalizeId(String enchantId) {
        return enchantId.toLowerCase(Locale.ROOT);
    }

    private String normalizeStoredId(String enchantId) {
        String id = normalizeId(enchantId);
        int colon = id.indexOf(':');
        if (colon > 0 && colon < id.length() - 1
                && EnchantmentRegistry.getNamespace().equals(id.substring(0, colon))) {
            return id.substring(colon + 1);
        }
        return id;
    }
}
