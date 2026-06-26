package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.FotiaEnchantment;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class GrindstoneDisableListener implements Listener {

    private final FotiaEnchantment plugin;

    public GrindstoneDisableListener(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isDisabled()) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.GRINDSTONE) {
            return;
        }

        event.setCancelled(true);
        sendDisabledMessage(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!isDisabled() || event.getInventory().getType() != InventoryType.GRINDSTONE) {
            return;
        }

        event.setCancelled(true);
        if (event.getPlayer() instanceof Player player) {
            sendDisabledMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isDisabled() || event.getInventory().getType() != InventoryType.GRINDSTONE) {
            return;
        }

        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            plugin.getServer().getScheduler().runTask(plugin, (Runnable) player::closeInventory);
            sendDisabledMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (isDisabled()) {
            event.setResult(null);
        }
    }

    private boolean isDisabled() {
        return plugin.getConfigManager().isGrindstoneDisabled();
    }

    private void sendDisabledMessage(Player player) {
        plugin.getMessageHelper().sendMessage(player, "grindstone-disabled");
    }
}
