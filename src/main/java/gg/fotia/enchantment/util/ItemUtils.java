package gg.fotia.enchantment.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * 物品构建工具类
 * 提供物品创建、PDC 标签操作、附魔光效等实用方法。
 */
public class ItemUtils {

    private static final JavaPlugin PLUGIN = JavaPlugin.getProvidingPlugin(ItemUtils.class);

    /**
     * 构建一个自定义物品
     *
     * @param material        材料类型
     * @param name            物品名称（Component）
     * @param lore            物品描述（Component 列表）
     * @param customModelData 自定义模型数据，0 表示不设置
     * @param glow            是否添加附魔光效
     * @return 构建好的物品
     */
    public static ItemStack buildItem(Material material, Component name, List<Component> lore, int customModelData, boolean glow) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (name != null) {
            meta.displayName(name);
        }

        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore);
        }

        if (customModelData != 0) {
            meta.setCustomModelData(customModelData);
        }

        item.setItemMeta(meta);

        if (glow) {
            addGlow(item);
        }

        return item;
    }

    /**
     * 设置物品的 item_model（1.21.4+ 特性）
     *
     * @param item  物品
     * @param model 模型标识符（如 "namespace:model_name"）
     */
    public static void setItemModel(ItemStack item, String model) {
        if (item == null || model == null || model.isEmpty()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = NamespacedKey.fromString(model);
        if (setItemModel(meta, key)) {
            item.setItemMeta(meta);
        }
    }

    /**
     * 设置物品的 tooltip_style（1.21.4+ 特性）
     *
     * @param item  物品
     * @param style tooltip 样式标识符
     */
    public static void setTooltipStyle(ItemStack item, String style) {
        if (item == null || style == null || style.isEmpty()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = NamespacedKey.fromString(style);
        if (setTooltipStyle(meta, key)) {
            item.setItemMeta(meta);
        }
    }

    public static boolean setItemModel(ItemMeta meta, NamespacedKey key) {
        if (meta == null || key == null) return false;
        try {
            meta.setItemModel(key);
            return true;
        } catch (LinkageError | UnsupportedOperationException ignored) {
            return false;
        }
    }

    public static boolean setTooltipStyle(ItemMeta meta, NamespacedKey key) {
        if (meta == null || key == null) return false;
        try {
            meta.setTooltipStyle(key);
            return true;
        } catch (LinkageError | UnsupportedOperationException ignored) {
            return false;
        }
    }

    /**
     * 为物品添加附魔光效（不显示附魔文本）
     *
     * @param item 物品
     */
    public static void addGlow(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
    }

    /**
     * 检查物品是否具有指定的 PDC 标签
     *
     * @param item 物品
     * @param key  标签键名
     * @return 是否存在该标签
     */
    public static boolean hasCustomTag(ItemStack item, String key) {
        if (item == null || key == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        NamespacedKey namespacedKey = new NamespacedKey(PLUGIN, key);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(namespacedKey, PersistentDataType.STRING);
    }

    /**
     * 为物品设置 PDC 标签
     *
     * @param item  物品
     * @param key   标签键名
     * @param value 标签值
     */
    public static void setCustomTag(ItemStack item, String key, String value) {
        if (item == null || key == null || value == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey namespacedKey = new NamespacedKey(PLUGIN, key);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(namespacedKey, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
    }

    /**
     * 获取物品的 PDC 标签值
     *
     * @param item 物品
     * @param key  标签键名
     * @return 标签值，不存在时返回 null
     */
    public static String getCustomTag(ItemStack item, String key) {
        if (item == null || key == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        NamespacedKey namespacedKey = new NamespacedKey(PLUGIN, key);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(namespacedKey, PersistentDataType.STRING);
    }
}
