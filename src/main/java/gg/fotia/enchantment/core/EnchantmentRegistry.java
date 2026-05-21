package gg.fotia.enchantment.core;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 附魔注册表 - PDC-only方案
 *
 * <p>不依赖Paper的实验性Registry API，附魔数据完全存储在物品的PersistentDataContainer中。
 * 使用 NamespacedKey("fotia", enchantmentId) 标识每个附魔。
 *
 * <p>本类负责维护附魔ID与NamespacedKey之间的映射关系，
 * 以及所有已注册附魔的索引。
 */
public class EnchantmentRegistry {

    private static final String NAMESPACE = "fotiaenchantment";

    private final Plugin plugin;

    /** 已注册附魔的 ID → NamespacedKey 映射 */
    private final Map<String, NamespacedKey> registeredKeys = new LinkedHashMap<>();

    /** 已注册附魔的 ID → EnchantmentData 映射 */
    private final Map<String, EnchantmentData> registeredEnchantments = new LinkedHashMap<>();

    public EnchantmentRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 注册一个附魔到注册表
     *
     * @param data 附魔数据
     * @return 是否注册成功（ID重复则返回false）
     */
    public boolean register(EnchantmentData data) {
        if (data == null || data.getId() == null || data.getId().isEmpty()) {
            return false;
        }
        String id = data.getId();
        if (registeredEnchantments.containsKey(id)) {
            plugin.getLogger().warning("附魔 ID 已注册: " + id);
            return false;
        }
        NamespacedKey key = new NamespacedKey(NAMESPACE, id);
        registeredKeys.put(id, key);
        registeredEnchantments.put(id, data);
        return true;
    }

    /**
     * 批量注册附魔
     *
     * @param enchantments 附魔数据集合
     * @return 成功注册的数量
     */
    public int registerAll(Collection<EnchantmentData> enchantments) {
        if (enchantments == null) {
            return 0;
        }
        int count = 0;
        for (EnchantmentData data : enchantments) {
            if (register(data)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 注销指定附魔
     *
     * @param id 附魔ID
     */
    public void unregister(String id) {
        if (id == null) {
            return;
        }
        registeredKeys.remove(id);
        registeredEnchantments.remove(id);
    }

    /**
     * 清空所有已注册附魔
     */
    public void unregisterAll() {
        registeredKeys.clear();
        registeredEnchantments.clear();
    }

    /**
     * 获取附魔的 NamespacedKey
     *
     * @param id 附魔ID
     * @return NamespacedKey，未注册返回null
     */
    public NamespacedKey getKey(String id) {
        return registeredKeys.get(id);
    }

    /**
     * 根据 NamespacedKey 查找附魔ID
     *
     * @param key NamespacedKey
     * @return 附魔ID，未找到返回null
     */
    public String getIdByKey(NamespacedKey key) {
        if (key == null || !NAMESPACE.equals(key.getNamespace())) {
            return null;
        }
        String possibleId = key.getKey();
        return registeredEnchantments.containsKey(possibleId) ? possibleId : null;
    }

    /**
     * 检查附魔是否已注册
     *
     * @param id 附魔ID
     * @return 是否已注册
     */
    public boolean isRegistered(String id) {
        return id != null && registeredEnchantments.containsKey(id);
    }

    /**
     * 获取已注册附魔数据
     *
     * @param id 附魔ID
     * @return 附魔数据，未注册返回null
     */
    public EnchantmentData getEnchantment(String id) {
        return registeredEnchantments.get(id);
    }

    /**
     * 获取所有已注册附魔（不可修改视图）
     */
    public Map<String, EnchantmentData> getAllEnchantments() {
        return Collections.unmodifiableMap(registeredEnchantments);
    }

    /**
     * 获取所有已注册的 NamespacedKey（不可修改视图）
     */
    public Map<String, NamespacedKey> getAllKeys() {
        return Collections.unmodifiableMap(registeredKeys);
    }

    /**
     * 获取已注册附魔数量
     */
    public int size() {
        return registeredEnchantments.size();
    }

    /**
     * 获取命名空间
     */
    public static String getNamespace() {
        return NAMESPACE;
    }
}
