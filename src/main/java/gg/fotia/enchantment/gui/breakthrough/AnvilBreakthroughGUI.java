package gg.fotia.enchantment.gui.breakthrough;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.gui.BaseGUI;
import gg.fotia.enchantment.gui.menu.MenuConfig;
import gg.fotia.enchantment.gui.menu.MenuItemConfig;
import gg.fotia.enchantment.listener.AnvilBreakthroughService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AnvilBreakthroughGUI extends BaseGUI {

    private static final String MENU_ID = "anvil-breakthrough";

    private final NamespacedKey actionTagKey;
    private final AnvilBreakthroughService service;
    private ItemStack target;
    private ItemStack book;
    private ItemStack catalyst;
    private MenuConfig menu;
    private boolean executed;

    public AnvilBreakthroughGUI(FotiaEnchantment plugin, Player player, ItemStack catalyst) {
        super(plugin, player);
        this.catalyst = normalize(catalyst);
        this.actionTagKey = new NamespacedKey(plugin, "anvil_breakthrough_action");
        this.service = new AnvilBreakthroughService(plugin);
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

        renderInput("target", InputTarget.TARGET, target, Material.DIAMOND_SWORD,
                "lang:anvil-breakthrough-gui.place-target");
        renderInput("book", InputTarget.BOOK, book, Material.ENCHANTED_BOOK,
                "lang:anvil-breakthrough-gui.place-book");
        renderInput("catalyst", InputTarget.CATALYST, catalyst, Material.ECHO_SHARD,
                "lang:anvil-breakthrough-gui.place-catalyst");
        renderPreview();
        renderControls();
        fillMenuBackground(menu);
    }

    private void renderInput(String itemId,
                             InputTarget targetType,
                             ItemStack input,
                             Material fallbackMaterial,
                             String defaultName) {
        int slot = slotOf(targetType);
        if (!isValidSlot(slot)) {
            return;
        }
        if (input != null) {
            inventory.setItem(slot, input.clone());
            return;
        }
        MenuItemConfig itemConfig = menu.item(itemId, fallbackMaterial);
        inventory.setItem(slot, item(itemConfig,
                menuText(defaultText(itemConfig.name(), defaultName), Collections.emptyMap()),
                menuLore(itemConfig, Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap())));
    }

    private void renderPreview() {
        int slot = menu.roleSlot("preview", 24);
        if (!isValidSlot(slot)) {
            return;
        }

        AnvilBreakthroughService.Preview preview = service.preview(player, target, book);
        if (preview.success()) {
            inventory.setItem(slot, preview.result().clone());
            return;
        }

        MenuItemConfig itemConfig = menu.item("preview", Material.BARRIER);
        Map<String, List<String>> listPlaceholders = Map.of("status_lines", statusLines(preview));
        inventory.setItem(slot, item(itemConfig,
                menuText(defaultText(itemConfig.name(), "lang:anvil-breakthrough-gui.preview"), Collections.emptyMap()),
                menuLore(itemConfig, List.of("{status_lines}"), Collections.emptyMap(), listPlaceholders)));
    }

    private void renderControls() {
        Map<String, List<String>> lists = Map.of("status_lines", statusLines(service.preview(player, target, book)));

        renderAction("cancel", "cancel", Material.RED_STAINED_GLASS_PANE,
                menu.roleSlot("cancel", 29),
                "lang:anvil-breakthrough-gui.cancel",
                List.of("lang:anvil-breakthrough-gui.cancel-hint"),
                Collections.emptyMap());

        renderAction("info", "info", Material.PAPER,
                menu.roleSlot("info", 31),
                "lang:anvil-breakthrough-gui.info-title",
                List.of("{status_lines}"),
                lists);

        renderAction("confirm", "confirm", Material.LIME_STAINED_GLASS_PANE,
                menu.roleSlot("confirm", 33),
                "lang:anvil-breakthrough-gui.confirm",
                List.of("lang:anvil-breakthrough-gui.confirm-hint", "{status_lines}"),
                lists);
    }

    private void renderAction(String itemId,
                              String action,
                              Material fallbackMaterial,
                              int slot,
                              String defaultName,
                              List<String> defaultLore,
                              Map<String, List<String>> listPlaceholders) {
        if (!isValidSlot(slot)) {
            return;
        }
        MenuItemConfig itemConfig = menu.item(itemId, fallbackMaterial);
        ItemStack item = item(itemConfig,
                menuText(defaultText(itemConfig.name(), defaultName), Collections.emptyMap()),
                menuLore(itemConfig, defaultLore, Collections.emptyMap(), listPlaceholders));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(actionTagKey, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
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

        int rawSlot = event.getRawSlot();
        for (InputTarget inputTarget : InputTarget.values()) {
            if (isInputSlot(inputTarget, rawSlot)) {
                handleInputClick(inputTarget);
                return;
            }
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }
        ItemMeta clickedMeta = clicked.getItemMeta();
        if (clickedMeta == null) {
            return;
        }
        String action = clickedMeta.getPersistentDataContainer().get(actionTagKey, PersistentDataType.STRING);
        if ("cancel".equals(action)) {
            player.closeInventory();
            return;
        }
        if ("confirm".equals(action)) {
            doBreakthrough();
        }
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        if (!executed) {
            returnInputs();
        }
    }

    private void doBreakthrough() {
        AnvilBreakthroughService.Preview preview = service.preview(player, target, book);
        if (catalyst == null || !preview.success()) {
            plugin.getMessageHelper().sendMessage(player, "anvil-breakthrough-not-ready");
            refresh();
            return;
        }

        ItemStack result = preview.result().clone();
        ItemStack remainingBook = consumeOne(book);
        ItemStack remainingCatalyst = consumeOne(catalyst);

        target = null;
        book = null;
        catalyst = null;
        executed = true;

        giveOrDrop(result);
        giveOrDrop(remainingBook);
        giveOrDrop(remainingCatalyst);
        plugin.getMessageHelper().sendMessage(player, "anvil-breakthrough-success");
        player.closeInventory();
    }

    private void handleInputClick(InputTarget targetType) {
        ItemStack cursor = normalize(player.getItemOnCursor());
        ItemStack current = input(targetType);

        if (cursor == null) {
            if (current == null) {
                return;
            }
            player.setItemOnCursor(current.clone());
            setInput(targetType, null);
            refresh();
            return;
        }

        if (!canPlace(targetType, cursor)) {
            return;
        }

        player.setItemOnCursor(current == null ? new ItemStack(Material.AIR) : current.clone());
        setInput(targetType, cursor);
        refresh();
    }

    private void handlePlayerInventoryShiftClick(InventoryClickEvent event) {
        ItemStack current = normalize(event.getCurrentItem());
        if (current == null) {
            return;
        }

        if (catalyst == null && canPlace(InputTarget.CATALYST, current)) {
            catalyst = current;
            event.setCurrentItem(new ItemStack(Material.AIR));
            refresh();
            return;
        }
        if (book == null && canPlace(InputTarget.BOOK, current)) {
            book = current;
            event.setCurrentItem(new ItemStack(Material.AIR));
            refresh();
            return;
        }
        if (target == null && canPlace(InputTarget.TARGET, current)) {
            target = current;
            event.setCurrentItem(new ItemStack(Material.AIR));
            refresh();
        }
    }

    private boolean canPlace(InputTarget targetType, ItemStack item) {
        item = normalize(item);
        if (item == null) {
            return false;
        }
        return switch (targetType) {
            case TARGET -> !isCatalyst(item) && item.getAmount() == 1;
            case BOOK -> item.getType() == Material.ENCHANTED_BOOK && !bookEnchantmentsEmpty(item);
            case CATALYST -> isCatalyst(item);
        };
    }

    private boolean bookEnchantmentsEmpty(ItemStack item) {
        if (!plugin.getEnchantmentManager().getPdcManager().getEnchantments(item).isEmpty()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return !(meta instanceof EnchantmentStorageMeta storageMeta) || storageMeta.getStoredEnchants().isEmpty();
    }

    private boolean isCatalyst(ItemStack item) {
        return plugin.getCustomItemManager().isAnvilBreakthroughStone(item);
    }

    private List<String> statusLines(AnvilBreakthroughService.Preview preview) {
        List<String> lines = new ArrayList<>();
        if (target == null) {
            lines.add(menuText("lang:anvil-breakthrough-gui.status-missing-target", Collections.emptyMap()));
        } else if (target.getAmount() != 1) {
            lines.add(menuText("lang:anvil-breakthrough-gui.status-target-stack", Collections.emptyMap()));
        }
        if (book == null) {
            lines.add(menuText("lang:anvil-breakthrough-gui.status-missing-book", Collections.emptyMap()));
        } else if (bookEnchantmentsEmpty(book)) {
            lines.add(menuText("lang:anvil-breakthrough-gui.status-invalid-book", Collections.emptyMap()));
        }
        if (catalyst == null) {
            lines.add(menuText("lang:anvil-breakthrough-gui.status-missing-catalyst", Collections.emptyMap()));
        }
        if (!lines.isEmpty()) {
            return lines;
        }
        if (preview != null && preview.success()) {
            lines.add(menuText("lang:anvil-breakthrough-gui.status-ready", Collections.emptyMap()));
            return lines;
        }
        lines.add(menuText(statusKey(preview), Collections.emptyMap()));
        return lines;
    }

    private String statusKey(AnvilBreakthroughService.Preview preview) {
        if (preview == null || preview.failureReason() == null) {
            return "lang:anvil-breakthrough-gui.status-no-result";
        }
        return switch (preview.failureReason()) {
            case MISSING_TARGET -> "lang:anvil-breakthrough-gui.status-missing-target";
            case TARGET_STACK -> "lang:anvil-breakthrough-gui.status-target-stack";
            case MISSING_BOOK, EMPTY_BOOK -> "lang:anvil-breakthrough-gui.status-invalid-book";
            case NO_VALID_ENCHANTMENT -> "lang:anvil-breakthrough-gui.status-no-result";
        };
    }

    private ItemStack consumeOne(ItemStack item) {
        item = normalize(item);
        if (item == null) {
            return null;
        }
        int amount = item.getAmount() - 1;
        if (amount <= 0) {
            return null;
        }
        item.setAmount(amount);
        return item;
    }

    private void returnInputs() {
        giveOrDrop(target);
        giveOrDrop(book);
        giveOrDrop(catalyst);
        target = null;
        book = null;
        catalyst = null;
    }

    private void giveOrDrop(ItemStack item) {
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

    private ItemStack input(InputTarget targetType) {
        return switch (targetType) {
            case TARGET -> target;
            case BOOK -> book;
            case CATALYST -> catalyst;
        };
    }

    private void setInput(InputTarget targetType, ItemStack item) {
        switch (targetType) {
            case TARGET -> target = normalize(item);
            case BOOK -> book = normalize(item);
            case CATALYST -> catalyst = normalize(item);
        }
    }

    private boolean isInputSlot(InputTarget targetType, int slot) {
        return menu.roleSlots(targetType.role(), List.of(slotOf(targetType))).contains(slot);
    }

    private int slotOf(InputTarget targetType) {
        return switch (targetType) {
            case TARGET -> menu.roleSlot("target", 11);
            case BOOK -> menu.roleSlot("book", 15);
            case CATALYST -> menu.roleSlot("catalyst", 13);
        };
    }

    private boolean isValidSlot(int slot) {
        return inventory != null && slot >= 0 && slot < inventory.getSize();
    }

    private ItemStack normalize(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
            return null;
        }
        return item.clone();
    }

    private enum InputTarget {
        TARGET("target"),
        BOOK("book"),
        CATALYST("catalyst");

        private final String role;

        InputTarget(String role) {
            this.role = role;
        }

        String role() {
            return role;
        }
    }
}
