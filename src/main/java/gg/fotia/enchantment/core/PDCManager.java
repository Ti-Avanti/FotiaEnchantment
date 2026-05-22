package gg.fotia.enchantment.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.fotia.enchantment.lore.item.EnchantmentDisplayPolicy;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PDC管理器 - 通过 PersistentDataContainer 在物品上存储和读取自定义附魔数据。
 *
 * <p>存储格式: key=fotia:enchantments, value=JSON字符串 {"enchant_id":level, ...}
 */
public class PDCManager {

    private static final Gson GSON = new Gson();

    /** 存储附魔数据的 NamespacedKey */
    private final NamespacedKey enchantmentsKey;

    public PDCManager(Plugin plugin) {
        this.enchantmentsKey = new NamespacedKey(plugin, "enchantments");
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
            boolean removedLegacy = legacy.remove(normalized) != null;
            removedLegacy |= legacy.remove(EnchantmentRegistry.getNamespace() + ":" + normalized) != null;
            if (removedLegacy) {
                writeEnchantments(meta, legacy);
            }
            hideNativeEnchantDisplay(meta);
            item.setItemMeta(meta);
            return item;
        }

        Map<String, Integer> enchants = readEnchantments(meta);
        String normalized = normalizeId(enchantId);
        enchants.remove(EnchantmentRegistry.getNamespace() + ":" + normalized);
        enchants.put(normalized, level);
        writeEnchantments(meta, enchants);
        hideNativeEnchantDisplay(meta);
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
            } else {
                writeEnchantments(meta, enchants);
            }
            modified = true;
        }
        if (modified) {
            hideNativeEnchantDisplay(meta);
            item.setItemMeta(meta);
        }
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
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Collections.emptyMap();
        }
        Map<String, Integer> enchants = readEnchantments(meta);
        enchants.putAll(readTrueEnchantments(meta));
        return Collections.unmodifiableMap(enchants);
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
        return Collections.unmodifiableMap(readEnchantments(meta));
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
        Map<String, Integer> enchants = readEnchantments(meta);
        return enchants.containsKey(normalizeId(enchantId));
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
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        Enchantment trueEnchantment = resolveTrueEnchantment(enchantId);
        if (trueEnchantment != null) {
            int level = getTrueEnchantmentLevel(meta, trueEnchantment);
            if (level > 0) {
                return level;
            }
        }
        Map<String, Integer> enchants = readEnchantments(meta);
        return enchants.getOrDefault(normalizeId(enchantId), 0);
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
        List<Material> applicable = data.getApplicableItems();
        // 如果未配置适用物品列表，则默认适用所有
        if (applicable == null || applicable.isEmpty()) {
            return true;
        }
        return applicable.contains(item.getType());
    }

    /**
     * 检查物品上是否存在与指定附魔冲突的附魔
     *
     * @param item 目标物品
     * @param data 要检查的附魔数据
     * @return 是否存在冲突
     */
    public boolean hasConflict(ItemStack item, EnchantmentData data) {
        if (item == null || data == null) {
            return false;
        }
        List<String> conflicts = data.getConflicts();
        if (conflicts == null || conflicts.isEmpty()) {
            return false;
        }
        Map<String, Integer> existing = getEnchantments(item);
        if (existing.isEmpty()) {
            return false;
        }
        for (String conflict : conflicts) {
            if (existing.containsKey(conflict)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取存储附魔的 NamespacedKey
     */
    public NamespacedKey getEnchantmentsKey() {
        return enchantmentsKey;
    }

    // ==================== 内部方法 ====================

    /**
     * 从 ItemMeta 的 PDC 中读取附魔数据
     */
    private Map<String, Integer> readEnchantments(ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String json = pdc.get(enchantmentsKey, PersistentDataType.STRING);
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) {
                return new HashMap<>();
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
            return enchants;
        } catch (RuntimeException e) {
            // JSON解析失败，返回空Map
            return new HashMap<>();
        }
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
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            addCustomTrueEnchantment(result, entry.getKey(), entry.getValue());
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<Enchantment, Integer> entry : storageMeta.getStoredEnchants().entrySet()) {
                addCustomTrueEnchantment(result, entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private void hideNativeEnchantDisplay(ItemMeta meta) {
        boolean hasStoredEnchants = meta instanceof EnchantmentStorageMeta storageMeta
                && !storageMeta.getStoredEnchants().isEmpty();
        boolean hasLegacyCustomEnchants = !readEnchantments(meta).isEmpty();
        if (EnchantmentDisplayPolicy.shouldHideNativeEnchantments(
                meta.hasEnchants(),
                hasStoredEnchants,
                hasLegacyCustomEnchants)) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_STORED_ENCHANTS);
        }
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

    private Enchantment resolveTrueEnchantment(String enchantId) {
        if (enchantId == null || enchantId.isBlank()) {
            return null;
        }
        return Registry.ENCHANTMENT.get(new NamespacedKey(
                EnchantmentRegistry.getNamespace(),
                normalizeId(enchantId)));
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
