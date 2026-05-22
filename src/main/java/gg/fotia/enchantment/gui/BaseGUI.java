package gg.fotia.enchantment.gui;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.gui.menu.MenuConfig;
import gg.fotia.enchantment.gui.menu.MenuItemConfig;
import gg.fotia.enchantment.gui.menu.MenuText;
import gg.fotia.enchantment.util.ItemUtils;
import gg.fotia.enchantment.util.LegacyColorConverter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 自定义 GUI 抽象基类
 * <p>
 * 所有 FotiaEnchantment 内的 GUI 都应继承本类。
 * 由 {@link GUIManager} 统一调度生命周期, 子类只需实现:
 * <ul>
 *     <li>{@link #open()} - 构造并打开界面</li>
 *     <li>{@link #handleClick(InventoryClickEvent)} - 处理点击</li>
 *     <li>{@link #handleClose(InventoryCloseEvent)} - 处理关闭</li>
 * </ul>
 */
public abstract class BaseGUI {

    protected static final MiniMessage MINI = MiniMessage.miniMessage();

    protected final FotiaEnchantment plugin;
    protected final Player player;
    protected Inventory inventory;

    protected BaseGUI(FotiaEnchantment plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    /** 构造并展示该 GUI */
    public abstract void open();

    /** 处理点击事件 (始终已被 GUIManager 取消默认行为) */
    public abstract void handleClick(InventoryClickEvent event);

    /** 处理关闭事件 */
    public abstract void handleClose(InventoryCloseEvent event);

    public Player getPlayer() {
        return player;
    }

    public Inventory getInventory() {
        return inventory;
    }

    // ============================================
    // 工具方法
    // ============================================

    /**
     * 解析 MiniMessage + 旧颜色码 + {@code <!i>}
     */
    protected Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return MINI.deserialize(LegacyColorConverter.convert(text));
    }

    /**
     * 创建一个简单装饰物品
     *
     * @param material 材料
     * @param name     显示名 (MiniMessage)
     * @param lore     lore 文本列表 (MiniMessage)
     */
    protected ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        applyText(meta, name, lore);
        item.setItemMeta(meta);
        return item;
    }

    protected ItemStack item(MenuItemConfig config, String name, List<String> lore) {
        ItemStack item = new ItemStack(config.material());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        applyText(meta, name, lore);
        if (config.modelData() != null) {
            meta.setCustomModelData(config.modelData());
        }
        applyNamespacedKey(config.tooltipStyle(), key -> ItemUtils.setTooltipStyle(meta, key), "tooltip-style");
        applyNamespacedKey(config.itemModel(), key -> ItemUtils.setItemModel(meta, key), "itemmodel");
        if (config.glow()) {
            applyGlow(meta);
        }

        item.setItemMeta(meta);
        return item;
    }

    protected void applyGlow(ItemMeta meta) {
        meta.setEnchantmentGlintOverride(true);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "gui_glow"), PersistentDataType.BYTE, (byte) 1);
    }

    protected String menuText(String raw, Map<String, String> placeholders) {
        return MenuText.render(raw, this::lang, placeholders);
    }

    protected List<String> menuLore(
            MenuItemConfig itemConfig,
            List<String> defaultLore,
            Map<String, String> placeholders,
            Map<String, List<String>> listPlaceholders
    ) {
        List<String> lore = itemConfig.lore().isEmpty() ? defaultLore : itemConfig.lore();
        return MenuText.renderLore(lore, this::lang, placeholders, listPlaceholders);
    }

    protected String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    protected void fillMenuBackground(MenuConfig menu) {
        if (inventory == null || menu == null) {
            return;
        }
        MenuItemConfig background = menu.item("background", Material.BLACK_STAINED_GLASS_PANE);
        ItemStack pane = item(background,
                menuText(defaultText(background.name(), "<!i><dark_gray> "), Collections.emptyMap()),
                menuLore(background, Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap()));
        for (int slot : menu.roleSlots("background", Collections.emptyList())) {
            if (slot >= 0 && slot < inventory.getSize() && inventory.getItem(slot) == null) {
                inventory.setItem(slot, pane);
            }
        }
        if (menu.fillEmpty()) {
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, pane);
                }
            }
        }
    }

    private void applyText(ItemMeta meta, String name, List<String> lore) {
        if (name != null && !name.isEmpty()) {
            meta.displayName(parse(name));
        }
        if (lore != null && !lore.isEmpty()) {
            List<Component> components = new ArrayList<>(lore.size());
            for (String line : lore) {
                if (line == null || line.isEmpty()) {
                    components.add(Component.empty());
                } else {
                    components.add(parse(line));
                }
            }
            meta.lore(components);
        }
    }

    private void applyNamespacedKey(String raw, Consumer<NamespacedKey> setter, String fieldName) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            NamespacedKey key = NamespacedKey.fromString(raw, plugin);
            if (key == null) {
                plugin.getLogger().warning("无效的 GUI 物品 " + fieldName + ": " + raw);
                return;
            }
            setter.accept(key);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("无效的 GUI 物品 " + fieldName + ": " + raw);
        }
    }

    /**
     * 黑色染色玻璃面板装饰物品 (空白填充)
     */
    protected ItemStack blackPane() {
        return item(Material.BLACK_STAINED_GLASS_PANE, "<!i><dark_gray> ", null);
    }

    /**
     * 用黑色玻璃填充指定的槽位范围
     */
    protected void fillBackground(int from, int to) {
        if (inventory == null) return;
        ItemStack pane = blackPane();
        for (int i = from; i < to; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, pane);
            }
        }
    }

    /**
     * 用黑色玻璃填充全部空槽位
     */
    protected void fillEmpty() {
        if (inventory == null) return;
        fillBackground(0, inventory.getSize());
    }

    /**
     * 从 gui.yml 语言文件读取文本
     */
    protected String lang(String key) {
        return plugin.getLanguageManager().getGUIText(player, key);
    }
}
