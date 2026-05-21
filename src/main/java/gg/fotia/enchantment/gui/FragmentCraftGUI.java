package gg.fotia.enchantment.gui;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.gui.menu.MenuConfig;
import gg.fotia.enchantment.gui.menu.MenuItemConfig;
import gg.fotia.enchantment.item.StarweaveFragment;
import gg.fotia.enchantment.item.StellarisCodex;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FragmentCraftGUI extends BaseGUI {

    private static final int DEFAULT_FRAGMENT_SLOT = 11;
    private static final int DEFAULT_ARROW_SLOT = 13;
    private static final int DEFAULT_RESULT_SLOT = 15;
    private static final int DEFAULT_CRAFT_SLOT = 22;

    private MenuConfig menu;
    private List<Integer> craftSlots = List.of(DEFAULT_CRAFT_SLOT);

    public FragmentCraftGUI(FotiaEnchantment plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void open() {
        menu = MenuConfig.from(plugin.getConfigManager().getGuiConfig("fragment-craft"), "fragment-craft");
        inventory = Bukkit.createInventory(null, menu.rows() * 9,
                parse(menuText(menu.title(), Collections.emptyMap())));
        refresh();
        player.openInventory(inventory);
    }

    private void refresh() {
        if (menu == null) {
            menu = MenuConfig.from(plugin.getConfigManager().getGuiConfig("fragment-craft"), "fragment-craft");
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }

        StarweaveFragment fragment = plugin.getCustomItemManager().getStarweaveFragment();
        int held = fragment.countInInventory(player);
        int cost = fragment.getFragmentCost();
        boolean enough = held >= cost;
        Map<String, String> placeholders = Map.of(
                "current", String.valueOf(held),
                "cost", String.valueOf(cost)
        );

        int fragmentSlot = menu.roleSlot("fragment", DEFAULT_FRAGMENT_SLOT);
        if (isValidSlot(fragmentSlot)) {
            inventory.setItem(fragmentSlot, fragment.create(player, Math.max(1, Math.min(64, held))));
        }

        int arrowSlot = menu.roleSlot("arrow", DEFAULT_ARROW_SLOT);
        if (isValidSlot(arrowSlot)) {
            MenuItemConfig arrow = menu.item("arrow", Material.ARROW);
            inventory.setItem(arrowSlot, item(arrow,
                    menuText(defaultText(arrow.name(), "lang:craft-gui.arrow"), placeholders),
                    menuLore(arrow, List.of("<!i><gray>{current}/{cost}"),
                            placeholders, Collections.emptyMap())));
        }

        int resultSlot = menu.roleSlot("result-preview", DEFAULT_RESULT_SLOT);
        if (isValidSlot(resultSlot)) {
            inventory.setItem(resultSlot, plugin.getCustomItemManager().createStellarisCodexPreview(player));
        }

        String craftItemId = enough ? "craft-ready" : "craft-insufficient";
        Material fallbackMaterial = enough ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        String fallbackName = enough ? "lang:craft-gui.craft-button" : "lang:craft-gui.insufficient-button";
        List<String> fallbackLore = new ArrayList<>();
        fallbackLore.add(enough ? "lang:craft-gui.ready" : "lang:craft-gui.insufficient");
        fallbackLore.add("lang:craft-gui.craft-hint");

        MenuItemConfig craftButton = menu.item(craftItemId, fallbackMaterial);
        ItemStack craftItem = item(craftButton,
                menuText(defaultText(craftButton.name(), fallbackName), placeholders),
                menuLore(craftButton, fallbackLore, placeholders, Collections.emptyMap()));
        craftSlots = validSlots(menu.roleSlots("craft-button", List.of(DEFAULT_CRAFT_SLOT)));
        for (int slot : craftSlots) {
            inventory.setItem(slot, craftItem);
        }

        fillMenuBackground(menu);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(inventory)) {
            return;
        }
        int raw = event.getRawSlot();
        if (!craftSlots.contains(raw)) {
            return;
        }

        StarweaveFragment fragment = plugin.getCustomItemManager().getStarweaveFragment();
        int held = fragment.countInInventory(player);
        int cost = fragment.getFragmentCost();
        if (held < cost) {
            refresh();
            return;
        }

        StellarisCodex codex = plugin.getCustomItemManager().getStellarisCodex();
        ItemStack result = codex.craft(player, held);
        if (result != null) {
            var leftover = player.getInventory().addItem(result);
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }
        refresh();
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
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
