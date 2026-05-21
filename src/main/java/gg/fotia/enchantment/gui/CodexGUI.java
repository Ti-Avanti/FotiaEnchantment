package gg.fotia.enchantment.gui;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.gui.menu.MenuConfig;
import gg.fotia.enchantment.gui.menu.MenuItemConfig;
import gg.fotia.enchantment.item.CustomItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodexGUI extends BaseGUI {

    private static final int DEFAULT_CODEX_SLOT = 13;
    private static final int DEFAULT_CONFIRM_SLOT = 22;

    private final ItemStack codex;
    private boolean confirmed = false;
    private MenuConfig menu;
    private List<Integer> confirmSlots = List.of(DEFAULT_CONFIRM_SLOT);

    public CodexGUI(FotiaEnchantment plugin, Player player, ItemStack codex) {
        super(plugin, player);
        this.codex = codex;
    }

    @Override
    public void open() {
        menu = MenuConfig.from(plugin.getConfigManager().getGuiConfig("codex"), "codex");
        inventory = Bukkit.createInventory(null, menu.rows() * 9,
                parse(menuText(menu.title(), Collections.emptyMap())));

        int codexSlot = menu.roleSlot("codex", DEFAULT_CODEX_SLOT);
        if (codex != null && isValidSlot(codexSlot)) {
            inventory.setItem(codexSlot, codex.clone());
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("rarity", "");
        placeholders.put("rarity_color", "<white>");
        placeholders.put("rarity_name", "");
        Map<String, List<String>> listPlaceholders = new HashMap<>();
        listPlaceholders.put("quality_line", Collections.emptyList());

        String rarity = plugin.getCustomItemManager().getCodexRarity(codex);
        if (rarity != null) {
            YamlConfiguration rarityConfig = plugin.getConfigManager().getRarityConfig();
            String color = rarityConfig.getString(rarity + ".color", "<white>");
            String name = plugin.getCustomItemManager().getRarityDisplayName(player, rarity);
            placeholders.put("rarity", rarity);
            placeholders.put("rarity_color", color);
            placeholders.put("rarity_name", name);
            listPlaceholders.put("quality_line",
                    List.of(menuText("lang:codex-gui.quality-hint", placeholders)));
        }

        MenuItemConfig confirm = menu.item("confirm", Material.LIME_STAINED_GLASS_PANE);
        ItemStack confirmItem = item(confirm,
                menuText(defaultText(confirm.name(), "lang:codex-gui.reveal-button"), placeholders),
                menuLore(confirm, List.of("lang:codex-gui.reveal-hint", "{quality_line}"),
                        placeholders, listPlaceholders));
        confirmSlots = validSlots(menu.roleSlots("confirm", List.of(DEFAULT_CONFIRM_SLOT)));
        for (int slot : confirmSlots) {
            inventory.setItem(slot, confirmItem);
        }

        fillMenuBackground(menu);
        player.openInventory(inventory);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory)) {
            return;
        }
        int raw = event.getRawSlot();
        if (confirmSlots.contains(raw) && !confirmed) {
            confirmed = doReveal();
            player.closeInventory();
        }
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        if (confirmed) {
            return;
        }
        if (codex != null) {
            var leftover = player.getInventory().addItem(codex.clone());
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }
    }

    private boolean doReveal() {
        CustomItemManager itemManager = plugin.getCustomItemManager();
        ItemStack book = itemManager.getStellarisCodex().reveal(player, codex);
        if (book != null) {
            var leftover = player.getInventory().addItem(book);
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
            return true;
        }
        return false;
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
