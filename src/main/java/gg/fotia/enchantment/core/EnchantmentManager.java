package gg.fotia.enchantment.core;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.config.EnchantmentConfig;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
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

    /** 附魔ID → EnchantmentData 缓存（启用与禁用都包含） */
    private final Map<String, EnchantmentData> enchantments = new LinkedHashMap<>();

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
        enchantments.clear();
    }

    /**
     * 重载所有附魔配置
     */
    public void reload() {
        registry.unregisterAll();
        enchantments.clear();
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
     * 获取所有附魔（不可修改视图）
     */
    public Collection<EnchantmentData> getAllEnchantments() {
        return Collections.unmodifiableCollection(enchantments.values());
    }

    /**
     * 按附魔组获取附魔列表
     *
     * @param group 组名
     * @return 属于该组的附魔列表
     */
    public List<EnchantmentData> getByCategory(String group) {
        if (group == null) {
            return Collections.emptyList();
        }
        String lowerGroup = group.toLowerCase(Locale.ROOT);
        return enchantments.values().stream()
                .filter(d -> lowerGroup.equals(
                        d.getGroup() != null ? d.getGroup().toLowerCase(Locale.ROOT) : ""))
                .collect(Collectors.toList());
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
    public void setEnabled(String id, boolean enabled) {
        EnchantmentData data = getEnchantment(id);
        if (data != null) {
            data.setEnabled(enabled);
        }
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
        List<EnchantmentData> result = new ArrayList<>();
        for (EnchantmentData data : enchantments.values()) {
            if (!data.isEnabled()) {
                continue;
            }
            if (pdcManager.isApplicable(item, data)) {
                result.add(data);
            }
        }
        return result;
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
            if (codexPools != null && codexPools.containsKey(lowerRarity)) {
                int weight = codexPools.get(lowerRarity);
                if (weight > 0) {
                    pool.put(data.getId(), weight);
                }
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

    // ==================== 内部方法 ====================

    /**
     * 重建内部缓存：从 EnchantmentConfig 加载数据到缓存和注册表
     */
    private void rebuildCache() {
        Collection<EnchantmentData> loaded = enchantmentConfig.getEnchantments();
        for (EnchantmentData data : loaded) {
            enchantments.put(data.getId(), data);
        }
        registry.registerAll(loaded);
    }
}
