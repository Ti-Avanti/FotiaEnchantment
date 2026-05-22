package gg.fotia.enchantment.gui.admin;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.gui.BaseGUI;
import gg.fotia.enchantment.gui.menu.MenuConfig;
import gg.fotia.enchantment.gui.menu.MenuItemConfig;
import gg.fotia.enchantment.gui.menu.MenuText;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 附魔管理 GUI
 * <p>
 * 布局和物品外观由 gui/admin.yml 驱动。点击附魔可切换启用 / 禁用状态。
 */
public class AdminGUI extends BaseGUI {

    private static final List<String> CATEGORIES = List.of(
            "all", "melee", "ranged", "armor", "tools", "universal"
    );

    /** 物品 PDC: 标记物品对应的附魔 ID */
    private final NamespacedKey enchantTagKey;
    private final NamespacedKey actionTagKey;

    private MenuConfig menu;
    private String currentCategory = "all";
    private int currentPage = 0;
    private List<EnchantmentData> currentList = new ArrayList<>();

    public AdminGUI(FotiaEnchantment plugin, Player player) {
        super(plugin, player);
        this.enchantTagKey = new NamespacedKey(plugin, "admin_gui_enchant");
        this.actionTagKey = new NamespacedKey(plugin, "admin_gui_action");
    }

    @Override
    public void open() {
        menu = MenuConfig.from(plugin.getConfigManager().getGuiConfig("admin"), "admin");
        String title = renderText(menu.title(), Collections.emptyMap());
        inventory = Bukkit.createInventory(null, menu.rows() * 9, parse(title));
        refresh();
        player.openInventory(inventory);
    }

    /**
     * 刷新整个 GUI
     */
    private void refresh() {
        if (menu == null) {
            menu = MenuConfig.from(plugin.getConfigManager().getGuiConfig("admin"), "admin");
        }
        // 清空
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }

        // 顶部类别标签
        renderCategoryTabs();

        // 附魔列表
        currentList = filterByCategory(currentCategory);
        renderEnchantList();

        // 底部按钮
        renderControls();
        renderShortcutHelp();

        // 填充背景
        fillConfiguredBackground();
    }

    private void renderCategoryTabs() {
        for (String cat : CATEGORIES) {
            int slot = menu.roleSlot("category-" + cat,
                    menu.slot("layout.categories." + cat, defaultCategorySlot(cat)));
            if (isValidSlot(slot)) {
                inventory.setItem(slot, createCategoryTab(cat));
            }
        }
    }

    private ItemStack createCategoryTab(String category) {
        boolean active = category.equals(currentCategory);
        MenuItemConfig itemConfig = menu.item("category-" + category, defaultCategoryMaterial(category));
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("category", category);
        placeholders.put("category_name", localizeCategory(category));
        placeholders.put("active_prefix", active ? "<!i><gold>▶ </gold>" : "");

        String name = renderText(defaultIfBlank(itemConfig.name(), "{active_prefix}{category_name}"), placeholders);
        List<String> lore = renderLore(itemConfig, List.of("lang:admin-gui.click-to-switch-category"),
                placeholders, Collections.emptyMap());
        ItemStack item = item(itemConfig, name, lore);
        if (active) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setEnchantmentGlintOverride(true);
                item.setItemMeta(meta);
            }
        }
        // 标记类别 ID
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "admin_gui_category"),
                    PersistentDataType.STRING,
                    category
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    private void renderEnchantList() {
        List<Integer> slots = validSlots(menu.enchantmentSlots());
        if (slots.isEmpty()) {
            return;
        }

        int pageSize = slots.size();
        int totalPages = Math.max(1, (int) Math.ceil(currentList.size() / (double) pageSize));
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, currentList.size());

        YamlConfiguration rarityConfig = plugin.getConfigManager().getRarityConfig();
        String enabledText = lang("admin-gui.enchant-enabled");
        String disabledText = lang("admin-gui.enchant-disabled");
        String toggleHint = lang("admin-gui.click-to-toggle");
        MenuItemConfig itemConfig = menu.item("enchantment", Material.ENCHANTED_BOOK);

        for (int i = start; i < end; i++) {
            EnchantmentData data = currentList.get(i);
            String enchantName = plugin.getLanguageManager().getEnchantName(player, data.getId());
            String rarityColor = "<white>";
            if (data.getRarity() != null) {
                rarityColor = rarityConfig.getString(data.getRarity() + ".color", "<white>");
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("enchant_id", data.getId());
            placeholders.put("enchant_name", enchantName);
            placeholders.put("rarity", data.getRarity() == null ? "" : data.getRarity());
            placeholders.put("rarity_color", rarityColor);
            placeholders.put("status", data.isEnabled() ? enabledText : disabledText);
            placeholders.put("toggle_hint", toggleHint);
            placeholders.put("max_level", String.valueOf(data.getMaxLevel()));
            placeholders.put("category", data.getCategory() == null ? "" : localizeCategory(data.getCategory()));

            Map<String, List<String>> listPlaceholders = new HashMap<>();
            if (data.getCategory() != null) {
                listPlaceholders.put("category_line", List.of(renderText("lang:admin-gui.category-label", placeholders)));
            } else {
                listPlaceholders.put("category_line", Collections.emptyList());
            }

            List<String> triggerLines = triggerLines(data);
            listPlaceholders.put("triggers", triggerLines);
            listPlaceholders.put("trigger_lines", triggerLinesWithLabel(triggerLines));

            String displayName = renderText(defaultIfBlank(itemConfig.name(), "<!i>{rarity_color}{enchant_name}"),
                    placeholders);
            List<String> lore = renderEnchantmentLore(itemConfig, List.of(
                    "{status}",
                    "lang:admin-gui.max-level",
                    "{category_line}",
                    "{trigger_lines}",
                    "",
                    "{toggle_hint}"
            ), placeholders, listPlaceholders);

            ItemStack book = item(itemConfig, displayName, lore);
            ItemMeta meta = book.getItemMeta();
            if (meta != null) {
                meta.setEnchantmentGlintOverride(data.isEnabled());
                meta.getPersistentDataContainer().set(
                        enchantTagKey, PersistentDataType.STRING, data.getId());
                book.setItemMeta(meta);
            }

            inventory.setItem(slots.get(i - start), book);
        }
    }

    private List<String> triggerLines(EnchantmentData data) {
        if (data.getEffects().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>();
        String lineTemplate = menu.root().getString("items.enchantment.trigger-line",
                "<!i><dark_gray>  - <gray>{trigger}");
        String moreTemplate = menu.root().getString("items.enchantment.trigger-more-line",
                "<!i><dark_gray>  ...");

        int shown = 0;
        for (EnchantmentData.EffectBlock block : data.getEffects()) {
            if (shown >= 3) break;
            lines.add(renderText(lineTemplate, Map.of("trigger", localizeTrigger(block.getTrigger()))));
            shown++;
        }
        if (data.getEffects().size() > 3) {
            lines.add(renderText(moreTemplate, Collections.emptyMap()));
        }
        return lines;
    }

    private List<String> triggerLinesWithLabel(List<String> triggerLines) {
        if (triggerLines.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>();
        lines.add(lang("admin-gui.triggers-label"));
        lines.addAll(triggerLines);
        return lines;
    }

    private String localizeCategory(String category) {
        String categoryKey = "admin-gui.category-" + category.toLowerCase(Locale.ROOT);
        String localized = lang(categoryKey);
        return categoryKey.equals(localized) ? category : localized;
    }

    private String localizeTrigger(String trigger) {
        String normalized = trigger.toUpperCase(Locale.ROOT);
        String triggerKey = "admin-gui.trigger-" + normalized;
        String localized = lang(triggerKey);
        if (!triggerKey.equals(localized)) {
            return localized;
        }
        return normalized.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private void renderControls() {
        int pageSize = Math.max(1, validSlots(menu.enchantmentSlots()).size());
        int totalPages = Math.max(1, (int) Math.ceil(currentList.size() / (double) pageSize));

        // 上一页
        if (currentPage > 0) {
            renderControl("previous-page", "previous", Material.ARROW,
                    menu.roleSlot("previous-page", menu.slot("layout.controls.previous", 45)),
                    "lang:admin-gui.prev-page",
                    List.of("lang:admin-gui.jump-page"),
                    Map.of(
                            "page", String.valueOf(currentPage),
                            "current", String.valueOf(currentPage + 1),
                            "total", String.valueOf(totalPages)
                    ));
        }

        // 关闭
        renderControl("close", "close", Material.BARRIER,
                menu.roleSlot("close", menu.slot("layout.controls.close", 49)),
                "lang:admin-gui.close",
                List.of("lang:admin-gui.close-hint", "lang:admin-gui.page-info"),
                Map.of(
                        "current", String.valueOf(currentPage + 1),
                        "total", String.valueOf(totalPages),
                        "page", String.valueOf(currentPage + 1)
                ));

        // 下一页
        if (currentPage < totalPages - 1) {
            renderControl("next-page", "next", Material.ARROW,
                    menu.roleSlot("next-page", menu.slot("layout.controls.next", 53)),
                    "lang:admin-gui.next-page",
                    List.of("lang:admin-gui.jump-page"),
                    Map.of(
                            "page", String.valueOf(currentPage + 2),
                            "current", String.valueOf(currentPage + 1),
                            "total", String.valueOf(totalPages)
                    ));
        }
    }

    private void renderShortcutHelp() {
        int slot = menu.roleSlot("shortcut-help", menu.slot("layout.controls.shortcut-help", 51));
        if (!isValidSlot(slot)) {
            return;
        }
        MenuItemConfig itemConfig = menu.item("shortcut-help", Material.KNOWLEDGE_BOOK);
        ItemStack item = item(itemConfig,
                renderText(defaultIfBlank(itemConfig.name(), "lang:admin-gui.shortcut-help"),
                        Collections.emptyMap()),
                renderLore(itemConfig, List.of(
                        "lang:admin-gui.shortcut-help-toggle",
                        "lang:admin-gui.shortcut-help-give-max",
                        "lang:admin-gui.shortcut-help-give-one"
                ), Collections.emptyMap(), Collections.emptyMap()));
        inventory.setItem(slot, item);
    }

    private void renderControl(
            String itemId,
            String action,
            Material fallbackMaterial,
            int slot,
            String defaultName,
            List<String> defaultLore,
            Map<String, String> placeholders
    ) {
        if (!isValidSlot(slot)) {
            return;
        }
        MenuItemConfig itemConfig = menu.item(itemId, fallbackMaterial);
        ItemStack item = item(itemConfig,
                renderText(defaultIfBlank(itemConfig.name(), defaultName), placeholders),
                renderLore(itemConfig, defaultLore, placeholders, Collections.emptyMap()));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(actionTagKey, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        Inventory clicked = event.getClickedInventory();
        if (clicked == null || !clicked.equals(inventory)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String action = pdc.get(actionTagKey, PersistentDataType.STRING);
            if (action != null) {
                switch (action) {
                    case "previous" -> {
                        currentPage--;
                        refresh();
                    }
                    case "next" -> {
                        currentPage++;
                        refresh();
                    }
                    case "close" -> player.closeInventory();
                    default -> {
                    }
                }
                return;
            }

            // 类别切换
            String cat = pdc.get(new NamespacedKey(plugin, "admin_gui_category"),
                    PersistentDataType.STRING);
            if (cat != null) {
                currentCategory = cat;
                currentPage = 0;
                refresh();
                return;
            }
            // 切换附魔启用状态
            String enchantId = pdc.get(enchantTagKey, PersistentDataType.STRING);
            if (enchantId != null) {
                EnchantmentData data = plugin.getEnchantmentManager().getEnchantment(enchantId);
                if (data != null) {
                    switch (AdminEnchantClickAction.from(event.getClick())) {
                        case TOGGLE -> {
                            data.setEnabled(!data.isEnabled());
                            refresh();
                        }
                        case GIVE_MAX_LEVEL_BOOK -> giveEnchantmentBook(data, data.getMaxLevel());
                        case GIVE_LEVEL_ONE_BOOK -> giveEnchantmentBook(data, 1);
                        case NONE -> {
                        }
                    }
                }
            }
        }
    }

    private void giveEnchantmentBook(EnchantmentData data, int level) {
        int safeLevel = Math.max(1, Math.min(level, data.getMaxLevel()));
        ItemStack book = plugin.getCustomItemManager().getStellarisCodex()
                .createEnchantedBook(player, data, safeLevel);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(book);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        String enchantName = plugin.getLanguageManager().getEnchantName(player, data.getId());
        plugin.getMessageHelper().sendMessage(player, "admin-gui-give-book-success", Map.of(
                "enchant_name", enchantName,
                "level", String.valueOf(safeLevel)
        ));
    }

    private void fillConfiguredBackground() {
        fillMenuBackground(menu);
    }

    private String renderText(String raw, Map<String, String> placeholders) {
        return MenuText.render(raw, this::lang, placeholders);
    }

    private List<String> renderLore(
            MenuItemConfig itemConfig,
            List<String> defaultLore,
            Map<String, String> placeholders,
            Map<String, List<String>> listPlaceholders
    ) {
        List<String> lore = itemConfig.lore().isEmpty() ? defaultLore : itemConfig.lore();
        return MenuText.renderLore(lore, this::lang, placeholders, listPlaceholders);
    }

    private List<String> renderEnchantmentLore(
            MenuItemConfig itemConfig,
            List<String> defaultLore,
            Map<String, String> placeholders,
            Map<String, List<String>> listPlaceholders
    ) {
        List<String> lore = itemConfig.lore().isEmpty() ? defaultLore : itemConfig.lore();
        lore = AdminShortcutHintLore.apply(lore,
                menu.root().getBoolean("items.enchantment.append-shortcut-hints", true));
        return MenuText.renderLore(lore, this::lang, placeholders, listPlaceholders);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private List<Integer> validSlots(List<Integer> slots) {
        List<Integer> result = new ArrayList<>();
        for (int slot : slots) {
            if (isValidSlot(slot)) {
                result.add(slot);
            }
        }
        return result;
    }

    private boolean isValidSlot(int slot) {
        return inventory != null && slot >= 0 && slot < inventory.getSize();
    }

    private int defaultCategorySlot(String category) {
        return switch (category) {
            case "all" -> 1;
            case "melee" -> 3;
            case "ranged" -> 4;
            case "armor" -> 5;
            case "tools" -> 6;
            case "universal" -> 7;
            default -> 0;
        };
    }

    private Material defaultCategoryMaterial(String category) {
        return switch (category) {
            case "melee" -> Material.IRON_SWORD;
            case "ranged" -> Material.BOW;
            case "armor" -> Material.IRON_CHESTPLATE;
            case "tools" -> Material.IRON_PICKAXE;
            case "universal" -> Material.NETHER_STAR;
            default -> Material.BOOKSHELF;
        };
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        // 无需特殊处理
    }

    /**
     * 按类别筛选附魔
     */
    private List<EnchantmentData> filterByCategory(String category) {
        List<EnchantmentData> all = new ArrayList<>(plugin.getEnchantmentManager().getAllEnchantments());
        if ("all".equalsIgnoreCase(category)) {
            return all;
        }
        List<EnchantmentData> result = new ArrayList<>();
        String lower = category.toLowerCase(Locale.ROOT);
        for (EnchantmentData data : all) {
            String cat = data.getCategory();
            String group = data.getGroup();
            if ((cat != null && cat.toLowerCase(Locale.ROOT).equals(lower))
                    || (group != null && group.toLowerCase(Locale.ROOT).equals(lower))) {
                result.add(data);
            }
        }
        return result;
    }
}
