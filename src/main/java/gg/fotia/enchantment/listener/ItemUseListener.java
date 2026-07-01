package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.gui.breakthrough.AnvilBreakthroughGUI;
import gg.fotia.enchantment.gui.codex.CodexGUI;
import gg.fotia.enchantment.gui.disenchant.DisenchantGUI;
import gg.fotia.enchantment.gui.fragment.FragmentCraftGUI;
import gg.fotia.enchantment.item.CustomItemManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ItemUseListener implements Listener {

    private final FotiaEnchantment plugin;
    private final CustomItemManager customItemManager;

    public ItemUseListener(FotiaEnchantment plugin) {
        this.plugin = plugin;
        this.customItemManager = plugin.getCustomItemManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.isEmpty()) {
            return;
        }

        String itemType = customItemManager.identifyItem(item);
        if (itemType == null) {
            return;
        }

        if (customItemManager.isDisenchantItemType(itemType)) {
            event.setCancelled(true);
            handleDisenchantStoneUse(player, item);
            return;
        }

        switch (itemType) {
            case "starweave-fragment" -> {
                event.setCancelled(true);
                plugin.getGuiManager().open(new FragmentCraftGUI(plugin, player));
            }
            case "stellaris-codex" -> {
                event.setCancelled(true);
                handleCodexUse(player, item);
            }
            case "anvil-breakthrough-stone" -> {
                event.setCancelled(true);
                handleAnvilBreakthroughUse(player, item);
            }
            default -> {
            }
        }
    }

    private void handleCodexUse(Player player, ItemStack codex) {
        ItemStack singleCodex = codex.clone();
        singleCodex.setAmount(1);
        codex.setAmount(codex.getAmount() - 1);
        plugin.getGuiManager().open(new CodexGUI(plugin, player, singleCodex));
    }

    private void handleDisenchantStoneUse(Player player, ItemStack stone) {
        ItemStack equipment = player.getInventory().getItemInOffHand();
        if (equipment != null && equipment.getType().isAir()) {
            equipment = null;
        }

        ItemStack guiStone = stone.clone();
        guiStone.setAmount(1);
        int remaining = stone.getAmount() - 1;
        if (remaining <= 0) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            stone.setAmount(remaining);
        }

        if (equipment != null) {
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        }
        plugin.getGuiManager().open(new DisenchantGUI(plugin, player, equipment, guiStone));
    }

    private void handleAnvilBreakthroughUse(Player player, ItemStack stone) {
        ItemStack guiStone = stone.clone();
        guiStone.setAmount(1);
        int remaining = stone.getAmount() - 1;
        if (remaining <= 0) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            stone.setAmount(remaining);
        }
        plugin.getGuiManager().open(new AnvilBreakthroughGUI(plugin, player, guiStone));
    }
}
