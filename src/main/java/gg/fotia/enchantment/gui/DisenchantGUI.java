package gg.fotia.enchantment.gui;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.gui.menu.MenuConfig;
import gg.fotia.enchantment.gui.menu.MenuItemConfig;
import gg.fotia.enchantment.item.DisenchantStone;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DisenchantGUI extends BaseGUI {

    private static final String MENU_ID = "disenchant";

    private final NamespacedKey enchantTagKey;
    private final NamespacedKey actionTagKey;

    private ItemStack equipment;
    private ItemStack stone;
    private final Set<String> selected = new HashSet<>();

    private MenuConfig menu;
    private boolean executed = false;

    public DisenchantGUI(FotiaEnchantment plugin, Player player, ItemStack equipment, ItemStack stone) {
        super(plugin, player);
        this.equipment = normalize(equipment);
        this.stone = normalize(stone);
        this.enchantTagKey = new NamespacedKey(plugin, "disenchant_gui_enchant");
        this.actionTagKey = new NamespacedKey(plugin, "disenchant_gui_action");
    }

    @Override
    public void open() {
        menu = MenuConfig.from(plugin.getConfigManager().getGuiConfig(MENU_ID), MENU_ID);
        inventory = Bukkit.createInventory(null, menu.rows() * 9,
                parse(menuText(menu.title(), Collections.emptyMap())));
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

        renderEquipment();
        renderStone();
        renderEnchantList();
        renderControls();
        fillMenuBackground(menu);
    }

    private void renderEquipment() {
        int slot = menu.roleSlot("equipment", 4);
        if (equipment != null) {
            setIfValid(slot, equipment.clone());
            return;
        }

        MenuItemConfig itemConfig = menu.item("equipment", Material.ARMOR_STAND);
        setIfValid(slot, item(itemConfig,
                menuText(defaultText(itemConfig.name(), "lang:disenchant-gui.place-item"), Collections.emptyMap()),
                menuLore(itemConfig, Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap())));
    }

    private void renderStone() {
        int slot = menu.roleSlot("stone", 7);
        if (stone != null) {
            setIfValid(slot, stone.clone());
            return;
        }

        MenuItemConfig itemConfig = menu.item("stone", Material.AMETHYST_SHARD);
        setIfValid(slot, item(itemConfig,
                menuText(defaultText(itemConfig.name(), "lang:disenchant-gui.place-stone"), Collections.emptyMap()),
                menuLore(itemConfig, Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap())));
    }

    private void renderControls() {
        DisenchantStone stoneLogic = plugin.getCustomItemManager().getDisenchantStone();
        String tier = currentTier();
        boolean selectable = isSelectable();
        String chance = String.valueOf(tier != null ? stoneLogic.getSuccessRate(tier) : 0);
        Map<String, String> infoPlaceholders = Map.of("chance", chance);
        Map<String, List<String>> infoLists = Map.of("mode_hint", List.of(
                selectable
                        ? menuText("lang:disenchant-gui.select-hint", infoPlaceholders)
                        : menuText("lang:disenchant-gui.random-hint", infoPlaceholders)));

        renderAction("cancel", "cancel", Material.RED_STAINED_GLASS_PANE,
                menu.roleSlot("cancel", 29),
                "lang:disenchant-gui.cancel",
                List.of("lang:disenchant-gui.cancel-hint"),
                Collections.emptyMap(),
                Collections.emptyMap());

        renderAction("info", "info", Material.PAPER,
                menu.roleSlot("info", 31),
                "lang:disenchant-gui.info-title",
                List.of("lang:disenchant-gui.success-chance", "{mode_hint}"),
                infoPlaceholders,
                infoLists);

        renderAction("confirm", "confirm", Material.LIME_STAINED_GLASS_PANE,
                menu.roleSlot("confirm", 33),
                "lang:disenchant-gui.confirm",
                List.of("lang:disenchant-gui.confirm-hint"),
                Collections.emptyMap(),
                Collections.emptyMap());
    }

    private void renderAction(
            String itemId,
            String action,
            Material fallbackMaterial,
            int slot,
            String defaultName,
            List<String> defaultLore,
            Map<String, String> placeholders,
            Map<String, List<String>> listPlaceholders
    ) {
        if (!isValidSlot(slot)) {
            return;
        }
        MenuItemConfig itemConfig = menu.item(itemId, fallbackMaterial);
        ItemStack item = item(itemConfig,
                menuText(defaultText(itemConfig.name(), defaultName), placeholders),
                menuLore(itemConfig, defaultLore, placeholders, listPlaceholders));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(actionTagKey, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private void renderEnchantList() {
        if (equipment == null) {
            return;
        }
        EnchantmentManager enchantManager = plugin.getEnchantmentManager();
        PDCManager pdc = enchantManager.getPdcManager();
        Map<String, Integer> enchants = pdc.getEnchantments(equipment);
        if (enchants.isEmpty()) {
            return;
        }

        List<Integer> slots = validSlots(menu.enchantmentSlots());
        if (slots.isEmpty()) {
            return;
        }

        DisenchantStone stoneLogic = plugin.getCustomItemManager().getDisenchantStone();
        YamlConfiguration rarityConfig = plugin.getConfigManager().getRarityConfig();
        MenuItemConfig itemConfig = menu.item("enchantment", Material.ENCHANTED_BOOK);

        int index = 0;
        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            if (index >= slots.size()) {
                break;
            }

            String id = entry.getKey();
            int level = entry.getValue();

            EnchantmentData data = enchantManager.getEnchantment(id);
            String name;
            String rarityColor = "<white>";
            if (data != null) {
                name = plugin.getLanguageManager().getEnchantName(player, id);
                if (data.getRarity() != null) {
                    rarityColor = rarityConfig.getString(data.getRarity() + ".color", "<white>");
                }
            } else {
                name = id;
            }

            String tier = currentTier();
            boolean selectable = isSelectable();
            boolean canRemove = tier != null && stoneLogic.canDisenchant(tier, id);
            boolean isSelected = selected.contains(id);
            Map<String, String> placeholders = Map.of(
                    "enchant_id", id,
                    "enchant_name", name,
                    "level", roman(level),
                    "rarity_color", rarityColor
            );

            List<String> statusLines = new ArrayList<>();
            if (!canRemove) {
                statusLines.add(menuText("lang:disenchant-gui.cannot-remove", placeholders));
            } else if (isSelected) {
                statusLines.add(menuText("lang:disenchant-gui.selected", placeholders));
            } else if (selectable) {
                statusLines.add(menuText("lang:disenchant-gui.select-hint", placeholders));
            }

            ItemStack book = item(itemConfig,
                    menuText(defaultText(itemConfig.name(), "lang:disenchant-gui.enchantment-name"), placeholders),
                    menuLore(itemConfig, List.of("{status_lines}"), placeholders,
                            Map.of("status_lines", statusLines)));

            ItemMeta meta = book.getItemMeta();
            if (meta != null) {
                meta.setEnchantmentGlintOverride(isSelected);
                meta.getPersistentDataContainer().set(enchantTagKey, PersistentDataType.STRING, id);
                book.setItemMeta(meta);
            }
            inventory.setItem(slots.get(index++), book);
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            return;
        }

        if (!event.getClickedInventory().equals(inventory)) {
            if (event.isShiftClick()) {
                handlePlayerInventoryShiftClick(event);
            }
            return;
        }

        ItemStack clicked = event.getCurrentItem();

        int rawSlot = event.getRawSlot();
        if (isEquipmentSlot(rawSlot)) {
            handleInputClick(InputTarget.EQUIPMENT);
            return;
        }
        if (isStoneSlot(rawSlot)) {
            handleInputClick(InputTarget.STONE);
            return;
        }

        if (clicked == null) {
            return;
        }

        ItemMeta clickedMeta = clicked.getItemMeta();
        if (clickedMeta != null) {
            String action = clickedMeta.getPersistentDataContainer().get(actionTagKey, PersistentDataType.STRING);
            if ("cancel".equals(action)) {
                player.closeInventory();
                return;
            }
            if ("confirm".equals(action)) {
                doDisenchant();
                return;
            }
        }

        if (!validSlots(menu.enchantmentSlots()).contains(rawSlot) || !isSelectable()) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String id = pdc.get(enchantTagKey, PersistentDataType.STRING);
        if (id == null) {
            return;
        }

        DisenchantStone stoneLogic = plugin.getCustomItemManager().getDisenchantStone();
        String tier = currentTier();
        if (tier == null) {
            return;
        }
        if (!stoneLogic.canDisenchant(tier, id)) {
            return;
        }
        int max = stoneLogic.getMaxRemoveCount(tier);
        if (selected.contains(id)) {
            selected.remove(id);
        } else if (selected.size() < max) {
            selected.add(id);
        }
        refresh();
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        returnInputs();
    }

    private void doDisenchant() {
        String tier = currentTier();
        if (executed || equipment == null || stone == null || tier == null) {
            return;
        }

        DisenchantStone stoneLogic = plugin.getCustomItemManager().getDisenchantStone();
        List<String> selectedIds = isSelectable() ? new ArrayList<>(selected) : null;
        List<ItemStack> books = stoneLogic.disenchant(player, equipment, stone, selectedIds);
        consumeOneStone();

        for (ItemStack book : books) {
            var leftover = player.getInventory().addItem(book);
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }

        executed = true;
        player.closeInventory();
    }

    private void handleInputClick(InputTarget target) {
        ItemStack cursor = normalize(player.getItemOnCursor());
        ItemStack current = target == InputTarget.EQUIPMENT ? equipment : stone;

        if (cursor == null) {
            if (current == null) {
                return;
            }
            player.setItemOnCursor(current.clone());
            setInput(target, null);
            refresh();
            return;
        }

        if (!canPlace(target, cursor)) {
            return;
        }

        player.setItemOnCursor(current == null ? new ItemStack(Material.AIR) : current.clone());
        setInput(target, cursor);
        refresh();
    }

    private void handlePlayerInventoryShiftClick(InventoryClickEvent event) {
        ItemStack current = normalize(event.getCurrentItem());
        if (current == null) {
            return;
        }

        DisenchantStone stoneLogic = plugin.getCustomItemManager().getDisenchantStone();
        if (stone == null && stoneLogic.isDisenchantStone(current)) {
            stone = current;
            event.setCurrentItem(new ItemStack(Material.AIR));
            selected.clear();
            refresh();
            return;
        }

        if (equipment == null && !stoneLogic.isDisenchantStone(current)) {
            equipment = current;
            event.setCurrentItem(new ItemStack(Material.AIR));
            selected.clear();
            refresh();
        }
    }

    private boolean canPlace(InputTarget target, ItemStack item) {
        DisenchantStone stoneLogic = plugin.getCustomItemManager().getDisenchantStone();
        if (target == InputTarget.STONE) {
            return stoneLogic.isDisenchantStone(item);
        }
        return !stoneLogic.isDisenchantStone(item);
    }

    private void setInput(InputTarget target, ItemStack item) {
        if (target == InputTarget.EQUIPMENT) {
            equipment = normalize(item);
        } else {
            stone = normalize(item);
        }
        selected.clear();
    }

    private void returnInputs() {
        returnInput(equipment);
        returnInput(stone);
        equipment = null;
        stone = null;
        selected.clear();
    }

    private void returnInput(ItemStack item) {
        item = normalize(item);
        if (item == null) {
            return;
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        if (!leftovers.isEmpty()) {
            for (ItemStack drop : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    private void consumeOneStone() {
        if (stone == null) {
            return;
        }
        int amount = stone.getAmount() - 1;
        if (amount <= 0) {
            stone = null;
        } else {
            stone.setAmount(amount);
        }
    }

    private String currentTier() {
        if (stone == null) {
            return null;
        }
        return plugin.getCustomItemManager().getDisenchantStone().getStoneTier(stone);
    }

    private boolean isSelectable() {
        String tier = currentTier();
        return tier != null && plugin.getCustomItemManager().getDisenchantStone().canSelect(tier);
    }

    private boolean isEquipmentSlot(int slot) {
        return menu.roleSlots("equipment", List.of(menu.roleSlot("equipment", 4))).contains(slot);
    }

    private boolean isStoneSlot(int slot) {
        return menu.roleSlots("stone", List.of(menu.roleSlot("stone", 7))).contains(slot);
    }

    private ItemStack normalize(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
            return null;
        }
        return item.clone();
    }

    private enum InputTarget {
        EQUIPMENT,
        STONE
    }

    private String roman(int num) {
        if (num <= 0) {
            return String.valueOf(num);
        }
        String[] ones = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (num <= 10) {
            return ones[num];
        }
        return String.valueOf(num);
    }

    private void setIfValid(int slot, ItemStack item) {
        if (isValidSlot(slot)) {
            inventory.setItem(slot, item);
        }
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
}
