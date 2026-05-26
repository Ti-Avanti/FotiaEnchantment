package gg.fotia.enchantment.gui.guide;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.gui.BaseGUI;
import gg.fotia.enchantment.gui.menu.MenuConfig;
import gg.fotia.enchantment.gui.menu.MenuItemConfig;
import gg.fotia.enchantment.lore.description.EnchantmentEffectDescriptionFormatter;
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

public class EnchantmentGuideGUI extends BaseGUI {

    private static final String MENU_ID = "enchantment-guide";
    private static final List<String> CATEGORIES = List.of(
            "all", "melee", "ranged", "armor", "tools", "universal"
    );

    private final NamespacedKey actionTagKey;
    private final NamespacedKey categoryTagKey;

    private MenuConfig menu;
    private String currentCategory = "all";
    private int currentPage = 0;
    private List<EnchantmentData> currentList = new ArrayList<>();

    public EnchantmentGuideGUI(FotiaEnchantment plugin, Player player) {
        super(plugin, player);
        this.actionTagKey = new NamespacedKey(plugin, "guide_gui_action");
        this.categoryTagKey = new NamespacedKey(plugin, "guide_gui_category");
    }

    @Override
    public void open() {
        menu = MenuConfig.from(plugin.getConfigManager().getGuiConfig(MENU_ID), MENU_ID);
        String title = menuText(menu.title(), Collections.emptyMap());
        inventory = Bukkit.createInventory(null, menu.rows() * 9, parse(title));
        refresh();
        player.openInventory(inventory);
    }

    private void refresh() {
        if (menu == null) {
            menu = MenuConfig.from(plugin.getConfigManager().getGuiConfig(MENU_ID), MENU_ID);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }

        renderCategoryTabs();
        currentList = filterByCategory(currentCategory);
        renderEnchantList();
        renderControls();
        fillMenuBackground(menu);
    }

    private void renderCategoryTabs() {
        for (String category : CATEGORIES) {
            int slot = menu.roleSlot("category-" + category, defaultCategorySlot(category));
            if (isValidSlot(slot)) {
                inventory.setItem(slot, createCategoryTab(category));
            }
        }
    }

    private ItemStack createCategoryTab(String category) {
        boolean active = category.equals(currentCategory);
        MenuItemConfig itemConfig = menu.item("category-" + category, defaultCategoryMaterial(category));
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("category", category);
        placeholders.put("category_name", localizeCategory(category));
        placeholders.put("active_prefix", active ? "<!i><gold>* </gold>" : "");

        ItemStack item = item(itemConfig,
                menuText(defaultText(itemConfig.name(), "{active_prefix}{category_name}"), placeholders),
                menuLore(itemConfig, List.of("lang:guide-gui.click-to-switch-category"),
                        placeholders, Collections.emptyMap()));

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (active) {
                applyGlow(meta);
            }
            meta.getPersistentDataContainer().set(categoryTagKey, PersistentDataType.STRING, category);
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
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }
        if (currentPage < 0) {
            currentPage = 0;
        }

        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, currentList.size());
        MenuItemConfig itemConfig = menu.item("enchantment", Material.ENCHANTED_BOOK);

        for (int i = start; i < end; i++) {
            EnchantmentData data = currentList.get(i);
            Map<String, String> placeholders = enchantmentPlaceholders(data);
            Map<String, List<String>> listPlaceholders = enchantmentListPlaceholders(data);

            ItemStack book = item(itemConfig,
                    menuText(defaultText(itemConfig.name(), "lang:guide-gui.enchantment-name"), placeholders),
                    menuLore(itemConfig, defaultEnchantmentLore(), placeholders, listPlaceholders));

            ItemMeta meta = book.getItemMeta();
            if (meta != null) {
                applyGlow(meta);
                book.setItemMeta(meta);
            }
            inventory.setItem(slots.get(i - start), book);
        }
    }

    private Map<String, String> enchantmentPlaceholders(EnchantmentData data) {
        YamlConfiguration rarityConfig = plugin.getConfigManager().getRarityConfig();
        String rarity = data.getRarity() == null ? "unknown" : data.getRarity();
        String rarityColor = rarityConfig == null ? "<white>" : rarityConfig.getString(rarity + ".color", "<white>");
        String rarityName = rarityName(rarity);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("enchant_id", data.getId());
        placeholders.put("enchant_name", plugin.getLanguageManager().getEnchantName(player, data.getId()));
        placeholders.put("rarity", rarity);
        placeholders.put("rarity_color", rarityColor);
        placeholders.put("rarity_name", rarityName);
        placeholders.put("category", data.getCategory() == null ? localizeCategory("universal") : localizeCategory(data.getCategory()));
        placeholders.put("group", data.getGroup() == null ? lang("guide-gui.group-none") : data.getGroup());
        placeholders.put("max_level", String.valueOf(data.getMaxLevel()));
        placeholders.put("applicable", applicableSummary(data));
        return placeholders;
    }

    private Map<String, List<String>> enchantmentListPlaceholders(EnchantmentData data) {
        Map<String, List<String>> listPlaceholders = new HashMap<>();
        listPlaceholders.put("curse_line", data.isCurse()
                ? List.of(lang("guide-gui.curse-line"))
                : Collections.emptyList());
        List<String> descriptionLines = descriptionLines(data);
        listPlaceholders.put("description_lines", descriptionLines);
        List<String> effectLines = effectLinesWithLabel(effectLines(data));
        listPlaceholders.put("effect_lines", effectLines);
        listPlaceholders.put("trigger_lines", descriptionLines);
        return listPlaceholders;
    }

    private List<String> defaultEnchantmentLore() {
        return List.of(
                "lang:guide-gui.rarity-line",
                "lang:guide-gui.category-line",
                "lang:guide-gui.group-line",
                "lang:guide-gui.max-level",
                "lang:guide-gui.applicable-line",
                "{curse_line}",
                "",
                "{description_lines}",
                "",
                "{effect_lines}"
        );
    }

    private List<String> descriptionLines(EnchantmentData data) {
        List<String> lines = new ArrayList<>();
        lines.add(lang("guide-gui.description-label"));

        List<String> descriptions = plugin.getLanguageManager().getEnchantDescription(player, data.getId());
        if (descriptions.isEmpty()) {
            lines.add(lang("guide-gui.no-description"));
            return lines;
        }

        String lineTemplate = menu.root().getString("items.enchantment.description-line",
                "<!i><dark_gray>  - <gray>{description}");
        for (String description : descriptions) {
            lines.add(menuText(lineTemplate, Map.of("description", description)));
        }
        return lines;
    }

    private List<String> effectLines(EnchantmentData data) {
        if (data.getEffects().isEmpty()) {
            return Collections.emptyList();
        }

        return EnchantmentEffectDescriptionFormatter.renderLines(data, previewLevels(data), this::lang);
    }

    private List<Integer> previewLevels(EnchantmentData data) {
        int maxLevel = Math.max(1, data.getMaxLevel());
        List<Integer> levels = new ArrayList<>(maxLevel);
        for (int level = 1; level <= maxLevel; level++) {
            levels.add(level);
        }
        return levels;
    }

    private List<String> effectLinesWithLabel(List<String> effectLines) {
        if (effectLines.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>();
        lines.add(lang("guide-gui.effects-label"));
        lines.addAll(effectLines);
        return lines;
    }

    private void renderControls() {
        int pageSize = Math.max(1, validSlots(menu.enchantmentSlots()).size());
        int totalPages = Math.max(1, (int) Math.ceil(currentList.size() / (double) pageSize));

        if (currentPage > 0) {
            renderControl("previous-page", "previous", Material.ARROW,
                    menu.roleSlot("previous-page", 45),
                    "lang:guide-gui.prev-page",
                    List.of("lang:guide-gui.jump-page"),
                    Map.of(
                            "page", String.valueOf(currentPage),
                            "current", String.valueOf(currentPage + 1),
                            "total", String.valueOf(totalPages)
                    ));
        }

        renderControl("close", "close", Material.BARRIER,
                menu.roleSlot("close", 49),
                "lang:guide-gui.close",
                List.of("lang:guide-gui.close-hint", "lang:guide-gui.page-info"),
                Map.of(
                        "current", String.valueOf(currentPage + 1),
                        "total", String.valueOf(totalPages),
                        "page", String.valueOf(currentPage + 1)
                ));

        if (currentPage < totalPages - 1) {
            renderControl("next-page", "next", Material.ARROW,
                    menu.roleSlot("next-page", 53),
                    "lang:guide-gui.next-page",
                    List.of("lang:guide-gui.jump-page"),
                    Map.of(
                            "page", String.valueOf(currentPage + 2),
                            "current", String.valueOf(currentPage + 1),
                            "total", String.valueOf(totalPages)
                    ));
        }
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
                menuText(defaultText(itemConfig.name(), defaultName), placeholders),
                menuLore(itemConfig, defaultLore, placeholders, Collections.emptyMap()));
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
        if (clicked == null || !clicked.equals(inventory)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return;
        }

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

        String category = pdc.get(categoryTagKey, PersistentDataType.STRING);
        if (category != null) {
            currentCategory = category;
            currentPage = 0;
            refresh();
        }
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
    }

    private List<EnchantmentData> filterByCategory(String category) {
        List<EnchantmentData> all = new ArrayList<>(plugin.getEnchantmentManager().getEnabled());
        if ("all".equalsIgnoreCase(category)) {
            return all;
        }

        List<EnchantmentData> result = new ArrayList<>();
        String lower = category.toLowerCase(Locale.ROOT);
        for (EnchantmentData data : all) {
            String dataCategory = data.getCategory();
            String group = data.getGroup();
            if ((dataCategory != null && dataCategory.toLowerCase(Locale.ROOT).equals(lower))
                    || (group != null && group.toLowerCase(Locale.ROOT).equals(lower))) {
                result.add(data);
            }
        }
        return result;
    }

    private String localizeCategory(String category) {
        String normalized = category.toLowerCase(Locale.ROOT);
        String guideKey = "guide-gui.category-" + normalized;
        String localized = lang(guideKey);
        if (!guideKey.equals(localized)) {
            return localized;
        }

        String adminKey = "admin-gui.category-" + normalized;
        localized = lang(adminKey);
        return adminKey.equals(localized) ? category : localized;
    }

    private String localizeTrigger(String trigger) {
        if (trigger == null || trigger.isBlank()) {
            return "";
        }

        String normalized = trigger.toUpperCase(Locale.ROOT);
        String guideKey = "guide-gui.trigger-" + normalized;
        String localized = lang(guideKey);
        if (!guideKey.equals(localized)) {
            return localized;
        }

        String adminKey = "admin-gui.trigger-" + normalized;
        localized = lang(adminKey);
        if (!adminKey.equals(localized)) {
            return localized;
        }
        return normalized.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private String localizeCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            return "";
        }

        String normalized = condition.toLowerCase(Locale.ROOT);
        String guideKey = "guide-gui.condition-" + normalized;
        String localized = lang(guideKey);
        return guideKey.equals(localized) ? humanizeId(normalized) : localized;
    }

    private String localizeAction(String action) {
        if (action == null || action.isBlank()) {
            return "";
        }

        String normalized = action.toUpperCase(Locale.ROOT);
        String guideKey = "guide-gui.action-" + normalized;
        String localized = lang(guideKey);
        return guideKey.equals(localized) ? humanizeId(normalized) : localized;
    }

    private String humanizeId(String id) {
        String[] parts = id.toLowerCase(Locale.ROOT).split("_+");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
        }
        return String.join(" ", words);
    }

    private String rarityName(String rarity) {
        if ("unknown".equals(rarity)) {
            return lang("guide-gui.rarity-unknown");
        }
        String key = "rarity-" + rarity;
        String localized = plugin.getLanguageManager().getMessage(player, key);
        return key.equals(localized) ? rarity : localized;
    }

    private String applicableSummary(EnchantmentData data) {
        if (data.getApplicableItems() == null || data.getApplicableItems().isEmpty()) {
            return lang("guide-gui.applicable-all");
        }
        return menuText(lang("guide-gui.applicable-count"),
                Map.of("count", String.valueOf(data.getApplicableItems().size())));
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
}
