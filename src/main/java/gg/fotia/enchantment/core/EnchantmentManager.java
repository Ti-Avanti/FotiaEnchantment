package gg.fotia.enchantment.core;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.config.EnchantmentConfig;
import gg.fotia.enchantment.config.EnchantmentConfig.ConfigIssue;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 附魔管理器 - 统一管理附魔生命周期，整合 EnchantmentConfig + PDCManager + EnchantmentRegistry。
 *
 * <p>后续GUI、命令、效果管道均通过此管理器获取附魔数据。
 */
public class EnchantmentManager {

    private final FotiaEnchantment plugin;
    private final EnchantmentConfig enchantmentConfig;
    private final PDCManager pdcManager;
    private final EnchantmentRegistry registry;

    /**
     * 附魔ID → EnchantmentData 快照（启用与禁用都包含）。
     * 不可变 Map 整体原子替换, 供主线程与数据包线程安全并发读取;
     * 重载期间读到的是旧快照而非半填充状态。
     */
    private volatile Map<String, EnchantmentData> enchantments = Collections.emptyMap();

    /** 材质 → 适用附魔列表 惰性缓存, 重载/启停附魔时清空 */
    private final Map<Material, List<EnchantmentData>> applicableCache = new ConcurrentHashMap<>();

    private List<UndefinedConflict> undefinedConflicts = Collections.emptyList();

    public EnchantmentManager(FotiaEnchantment plugin) {
        this.plugin = plugin;
        this.enchantmentConfig = new EnchantmentConfig(plugin);
        this.pdcManager = new PDCManager(plugin);
        this.registry = new EnchantmentRegistry(plugin);
    }

    /**
     * 初始化附魔管理器：加载配置并注册所有附魔
     */
    public void init() {
        enchantmentConfig.loadAll();
        rebuildCache();
        plugin.getLogger().info("附魔管理器已初始化，共注册 " + registry.size() + " 个附魔");
    }

    /**
     * 关闭附魔管理器，清理资源
     */
    public void shutdown() {
        registry.unregisterAll();
        enchantments = Collections.emptyMap();
        applicableCache.clear();
    }

    /**
     * 重载所有附魔配置
     */
    public void reload() {
        registry.unregisterAll();
        enchantmentConfig.reload();
        rebuildCache();
        plugin.getLogger().info("附魔管理器已重载，共注册 " + registry.size() + " 个附魔");
    }

    /**
     * 按ID获取附魔数据
     *
     * @param id 附魔ID
     * @return 附魔数据，不存在返回null
     */
    public EnchantmentData getEnchantment(String id) {
        if (id == null) {
            return null;
        }
        return enchantments.get(id.toLowerCase(Locale.ROOT));
    }

    /**
     * 获取所有附魔（不可变快照视图）
     */
    public Collection<EnchantmentData> getAllEnchantments() {
        return enchantments.values();
    }

    /**
     * 按目录分类获取附魔列表 (melee/ranged/armor/tools/universal)。
     * 同时兼容按附魔组名(group)筛选, 与图鉴 GUI 的筛选行为一致。
     *
     * @param category 分类或组名
     * @return 属于该分类/组的附魔列表
     */
    public List<EnchantmentData> getByCategory(String category) {
        if (category == null) {
            return Collections.emptyList();
        }
        String lower = category.toLowerCase(Locale.ROOT);
        List<EnchantmentData> result = new ArrayList<>();
        for (EnchantmentData d : enchantments.values()) {
            String cat = d.getCategory();
            String group = d.getGroup();
            if ((cat != null && cat.toLowerCase(Locale.ROOT).equals(lower))
                    || (group != null && group.toLowerCase(Locale.ROOT).equals(lower))) {
                result.add(d);
            }
        }
        return result;
    }

    /**
     * 按稀有度获取附魔列表
     *
     * @param rarity 稀有度名称
     * @return 该稀有度的附魔列表
     */
    public List<EnchantmentData> getByRarity(String rarity) {
        if (rarity == null) {
            return Collections.emptyList();
        }
        String lowerRarity = rarity.toLowerCase(Locale.ROOT);
        return enchantments.values().stream()
                .filter(d -> lowerRarity.equals(
                        d.getRarity() != null ? d.getRarity().toLowerCase(Locale.ROOT) : ""))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有启用的附魔
     */
    public List<EnchantmentData> getEnabled() {
        return enchantments.values().stream()
                .filter(EnchantmentData::isEnabled)
                .collect(Collectors.toList());
    }

    /**
     * 启用或禁用指定附魔
     *
     * @param id      附魔ID
     * @param enabled 是否启用
     */
    public boolean setEnabled(String id, boolean enabled) {
        EnchantmentData data = getEnchantment(id);
        if (data == null) {
            return false;
        }
        boolean persisted = enchantmentConfig.setEnabled(id, enabled);
        if (!persisted) {
            data.setEnabled(enabled);
        }
        applicableCache.clear();
        if (plugin.getEffectPipeline() != null) {
            plugin.getEffectPipeline().rebuildTriggerIndex();
        }
        return persisted;
    }

    /**
     * 获取适用于指定物品的所有已启用附魔
     *
     * @param item 目标物品
     * @return 适用的附魔列表
     */
    public List<EnchantmentData> getApplicable(ItemStack item) {
        if (item == null) {
            return Collections.emptyList();
        }
        return getApplicable(item.getType());
    }

    /**
     * 获取适用于指定材质的所有已启用附魔 (按材质惰性缓存, 结果不可修改)
     */
    public List<EnchantmentData> getApplicable(Material type) {
        if (type == null) {
            return Collections.emptyList();
        }
        List<EnchantmentData> cached = applicableCache.get(type);
        if (cached != null) {
            return cached;
        }
        List<EnchantmentData> result = new ArrayList<>();
        for (EnchantmentData data : enchantments.values()) {
            if (!data.isEnabled()) {
                continue;
            }
            if (data.getApplicableItems().isEmpty() || data.isApplicableTo(type)) {
                result.add(data);
            }
        }
        List<EnchantmentData> immutable = result.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(result);
        applicableCache.put(type, immutable);
        return immutable;
    }

    /**
     * 获取指定稀有度的星芒魔典抽取池（附魔ID → 权重映射）
     *
     * <p>仅返回已启用的附魔。
     *
     * @param rarity 稀有度池名称（对应 codex-pools 中的 key）
     * @return 附魔ID→权重的映射
     */
    public Map<String, Integer> getCodexPool(String rarity) {
        if (rarity == null) {
            return Collections.emptyMap();
        }
        String lowerRarity = rarity.toLowerCase(Locale.ROOT);
        Map<String, Integer> pool = new HashMap<>();
        for (EnchantmentData data : enchantments.values()) {
            if (!data.isEnabled()) {
                continue;
            }
            Map<String, Integer> codexPools = data.getCodexPools();
            Integer weight = codexPools != null ? codexPools.get(lowerRarity) : null;
            if (weight != null && weight > 0) {
                pool.put(data.getId(), weight);
            }
        }
        return pool;
    }

    /**
     * 从指定稀有度池中按权重随机抽取一个附魔
     *
     * @param rarity 稀有度池名称
     * @return 抽中的附魔ID，池为空时返回null
     */
    public String rollCodex(String rarity) {
        Map<String, Integer> pool = getCodexPool(rarity);
        if (pool.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (int w : pool.values()) {
            totalWeight += w;
        }
        if (totalWeight <= 0) {
            return null;
        }

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (Map.Entry<String, Integer> entry : pool.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) {
                return entry.getKey();
            }
        }
        // 理论上不会到此，兜底返回最后一个
        return pool.keySet().iterator().next();
    }

    // ==================== Getter ====================

    /**
     * 获取PDC管理器
     */
    public PDCManager getPdcManager() {
        return pdcManager;
    }

    /**
     * 获取附魔注册表
     */
    public EnchantmentRegistry getRegistry() {
        return registry;
    }

    /**
     * 获取附魔配置加载器
     */
    public EnchantmentConfig getEnchantmentConfig() {
        return enchantmentConfig;
    }

    public List<UndefinedConflict> getUndefinedConflicts() {
        return undefinedConflicts;
    }

    public List<ConfigIssue> getConfigIssues() {
        return enchantmentConfig.getConfigIssues();
    }

    // ==================== 内部方法 ====================

    /**
     * 重建内部缓存：从 EnchantmentConfig 加载数据构建新快照后整体替换
     */
    private void rebuildCache() {
        Collection<EnchantmentData> loaded = enchantmentConfig.getEnchantments();
        Map<String, EnchantmentData> snapshot = new LinkedHashMap<>();
        for (EnchantmentData data : loaded) {
            snapshot.put(data.getId(), data);
        }
        enchantments = Collections.unmodifiableMap(snapshot);
        applicableCache.clear();
        registry.registerAll(loaded);
        undefinedConflicts = findUndefinedConflicts(snapshot.values(), id -> {
            File file = enchantmentConfig.getSourceFile(id);
            return file != null ? file.getAbsolutePath() : "未知文件";
        });
        logUndefinedConflicts(undefinedConflicts);
    }

    static List<UndefinedConflict> findUndefinedConflicts(Collection<EnchantmentData> enchantments,
                                                          Function<String, String> sourceFileResolver) {
        if (enchantments.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> definedIds = new LinkedHashSet<>();
        for (EnchantmentData data : enchantments) {
            if (data != null && data.getId() != null) {
                definedIds.add(data.getId());
            }
        }
        Set<String> seen = new LinkedHashSet<>();
        List<UndefinedConflict> result = new ArrayList<>();

        for (EnchantmentData data : enchantments) {
            if (data == null || data.getId() == null) {
                continue;
            }
            List<String> conflicts = data.getConflicts();
            if (conflicts == null || conflicts.isEmpty()) {
                continue;
            }
            for (String rawConflict : conflicts) {
                String conflictId = normalizeCustomConflictId(rawConflict);
                if (conflictId == null || definedIds.contains(conflictId)) {
                    continue;
                }

                String key = data.getId() + "::" + conflictId;
                if (seen.add(key)) {
                    result.add(new UndefinedConflict(data.getId(), conflictId, sourceFileResolver.apply(data.getId())));
                }
            }
        }

        return result.isEmpty() ? Collections.emptyList() : List.copyOf(result);
    }

    private static String normalizeCustomConflictId(String rawConflict) {
        if (rawConflict == null || rawConflict.isBlank()) {
            return null;
        }

        String id = rawConflict.trim().toLowerCase(Locale.ROOT);
        int separator = id.indexOf(':');
        if (separator < 0) {
            return id;
        }

        String namespace = id.substring(0, separator);
        if (!EnchantmentRegistry.getNamespace().equals(namespace) || separator >= id.length() - 1) {
            return null;
        }
        return id.substring(separator + 1);
    }

    private void logUndefinedConflicts(List<UndefinedConflict> conflicts) {
        for (UndefinedConflict conflict : conflicts) {
            plugin.getLogger().warning("附魔 " + conflict.sourceId()
                    + " 的 conflicts 引用了未定义附魔: " + conflict.conflictId()
                    + " (文件: " + conflict.sourceFile() + ", 位置: conflicts)");
        }
    }

    public record UndefinedConflict(String sourceId, String conflictId, String sourceFile) {
    }
}
