package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.lore.item.EnchantmentDisplayPolicy;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class EnchantmentDisplayListener implements Listener {

    private final FotiaEnchantment plugin;

    public EnchantmentDisplayListener(FotiaEnchantment plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::normalizeOnlinePlayers, 20L, 40L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        scheduleNormalize(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        scheduleNormalize(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        scheduleNormalize(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeld(PlayerItemHeldEvent event) {
        scheduleNormalize(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        scheduleNormalize(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            scheduleNormalize(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            normalizeView(player);
            scheduleNormalize(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleNormalize(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleNormalize(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleNormalize(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        scheduleNormalize(event.getEnchanter());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        scheduleNormalize(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        Bukkit.getScheduler().runTask(plugin, this::normalizeOnlinePlayers);
    }

    private void scheduleNormalize(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> normalizePlayer(player));
    }

    private void normalizeOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            normalizePlayer(player);
        }
    }

    private void normalizePlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        boolean changed = normalizeInventory(player.getInventory());
        ItemStack cursor = player.getItemOnCursor();
        if (normalizeItem(cursor)) {
            player.setItemOnCursor(cursor);
            changed = true;
        }
        changed |= normalizeView(player);

        if (changed) {
            player.updateInventory();
        }
    }

    private boolean normalizeView(Player player) {
        InventoryView view = player.getOpenInventory();
        if (view == null) {
            return false;
        }

        boolean changed = normalizeInventory(view.getTopInventory());
        Inventory bottom = view.getBottomInventory();
        if (!(bottom.getHolder() instanceof HumanEntity)) {
            changed |= normalizeInventory(bottom);
        }
        return changed;
    }

    private boolean normalizeInventory(Inventory inventory) {
        if (inventory == null) {
            return false;
        }

        boolean changed = false;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (normalizeItem(stack)) {
                inventory.setItem(slot, stack);
                changed = true;
            }
        }
        return changed;
    }

    private boolean normalizeItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }

        PDCManager pdc = pdcManager();
        if (pdc == null) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        boolean hasStoredEnchants = meta instanceof EnchantmentStorageMeta storageMeta
                && !storageMeta.getStoredEnchants().isEmpty();
        boolean hasLegacyCustomEnchants = !pdc.getLegacyEnchantments(item).isEmpty();
        if (!EnchantmentDisplayPolicy.shouldHideNativeEnchantments(
                meta.hasEnchants(),
                hasStoredEnchants,
                hasLegacyCustomEnchants)) {
            return false;
        }

        boolean changed = false;
        if (!meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            changed = true;
        }
        if (!meta.hasItemFlag(ItemFlag.HIDE_STORED_ENCHANTS)) {
            meta.addItemFlags(ItemFlag.HIDE_STORED_ENCHANTS);
            changed = true;
        }

        if (changed) {
            item.setItemMeta(meta);
        }
        return changed;
    }

    private PDCManager pdcManager() {
        EnchantmentManager manager = plugin.getEnchantmentManager();
        return manager == null ? null : manager.getPdcManager();
    }
}
