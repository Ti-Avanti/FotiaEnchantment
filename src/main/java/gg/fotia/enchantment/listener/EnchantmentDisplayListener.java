package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentItemSanitizer;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.lore.item.EnchantmentDisplayPolicy;
import gg.fotia.enchantment.lore.item.EnchantmentLoreCleaner;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EnchantmentDisplayListener implements Listener {

    private final FotiaEnchantment plugin;

    public EnchantmentDisplayListener(FotiaEnchantment plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::startAsyncValidityScan, 20L, itemValidityCheckInterval());
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
            normalizeView(player, validityRules());
            scheduleNormalize(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleNormalize(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            ItemStack cursor = event.getCursor();
            if (normalizeItem(player, cursor, validityRules())) {
                event.setCursor(cursor);
            }
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

    private void startAsyncValidityScan() {
        PDCManager pdc = pdcManager();
        EnchantmentManager manager = plugin.getEnchantmentManager();
        if (pdc == null || manager == null) {
            return;
        }

        EnchantmentItemSanitizer.ValidityRules rules =
                EnchantmentItemSanitizer.ValidityRules.from(manager.getAllEnchantments());
        List<PlayerItemSnapshot> snapshots = snapshotOnlinePlayers();
        if (snapshots.isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<UUID> playersToNormalize = new ArrayList<>();
            for (PlayerItemSnapshot snapshot : snapshots) {
                if (snapshot.requiresNormalization(pdc, rules)) {
                    playersToNormalize.add(snapshot.playerId());
                }
            }
            if (playersToNormalize.isEmpty()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                boolean changedAny = false;
                for (UUID playerId : playersToNormalize) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        normalizePlayer(player);
                        changedAny = true;
                    }
                }
                if (changedAny) {
                    plugin.getLogger().fine("Normalized enchantment data for " + playersToNormalize.size()
                            + " online player(s).");
                }
            });
        });
    }

    private void normalizePlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        EnchantmentItemSanitizer.ValidityRules rules = validityRules();
        boolean changed = normalizeInventory(player, player.getInventory(), rules);
        ItemStack cursor = player.getItemOnCursor();
        if (normalizeItem(player, cursor, rules)) {
            player.setItemOnCursor(cursor);
            changed = true;
        }
        changed |= normalizeView(player, rules);

        if (changed) {
            player.updateInventory();
        }
    }

    private boolean normalizeView(Player player, EnchantmentItemSanitizer.ValidityRules rules) {
        InventoryView view = player.getOpenInventory();
        if (view == null) {
            return false;
        }

        boolean changed = normalizeInventory(player, view.getTopInventory(), rules);
        Inventory bottom = view.getBottomInventory();
        if (!(bottom.getHolder() instanceof HumanEntity)) {
            changed |= normalizeInventory(player, bottom, rules);
        }
        return changed;
    }

    private boolean normalizeInventory(Player player,
                                       Inventory inventory,
                                       EnchantmentItemSanitizer.ValidityRules rules) {
        if (inventory == null) {
            return false;
        }

        boolean changed = false;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (normalizeItem(player, stack, rules)) {
                inventory.setItem(slot, stack);
                changed = true;
            }
        }
        return changed;
    }

    private boolean normalizeItem(Player player, ItemStack item, EnchantmentItemSanitizer.ValidityRules rules) {
        if (item == null || item.getType().isAir()) {
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

        boolean needsSanitization = EnchantmentItemSanitizer.needsSanitization(item, pdc, rules);
        boolean changed = needsSanitization && EnchantmentLoreCleaner.stripGeneratedLore(plugin, player, item);
        changed |= EnchantmentItemSanitizer.sanitize(plugin, item);

        meta = item.getItemMeta();
        if (meta == null) {
            return changed;
        }

        changed |= EnchantmentLoreCleaner.applyGeneratedLore(plugin, player, item);
        meta = item.getItemMeta();
        if (meta == null) {
            return changed;
        }

        boolean hasStoredEnchants = meta instanceof EnchantmentStorageMeta storageMeta
                && !storageMeta.getStoredEnchants().isEmpty();
        boolean hasLegacyCustomEnchants = !pdc.getLegacyEnchantments(item).isEmpty();
        if (!EnchantmentDisplayPolicy.shouldHideNativeEnchantments(
                meta.hasEnchants(),
                hasStoredEnchants,
                hasLegacyCustomEnchants)) {
            return changed;
        }

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

    private EnchantmentItemSanitizer.ValidityRules validityRules() {
        EnchantmentManager manager = plugin.getEnchantmentManager();
        return manager == null
                ? EnchantmentItemSanitizer.ValidityRules.from(List.of())
                : EnchantmentItemSanitizer.ValidityRules.from(manager.getAllEnchantments());
    }

    private long itemValidityCheckInterval() {
        if (plugin.getConfigManager() == null) {
            return 40L;
        }
        return Math.max(20L, plugin.getConfigManager().getItemValidityCheckInterval());
    }

    private List<PlayerItemSnapshot> snapshotOnlinePlayers() {
        List<PlayerItemSnapshot> snapshots = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != null && player.isOnline()) {
                snapshots.add(new PlayerItemSnapshot(player.getUniqueId(), snapshotItems(player)));
            }
        }
        return snapshots;
    }

    private List<ItemStack> snapshotItems(Player player) {
        List<ItemStack> items = new ArrayList<>();
        addInventoryContents(items, player.getInventory());
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null) {
            items.add(cursor.clone());
        }

        InventoryView view = player.getOpenInventory();
        if (view != null) {
            addInventoryContents(items, view.getTopInventory());
            Inventory bottom = view.getBottomInventory();
            if (!(bottom.getHolder() instanceof HumanEntity)) {
                addInventoryContents(items, bottom);
            }
        }
        return items;
    }

    private void addInventoryContents(List<ItemStack> items, Inventory inventory) {
        if (inventory == null) {
            return;
        }
        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                items.add(item.clone());
            }
        }
    }

    private record PlayerItemSnapshot(UUID playerId, List<ItemStack> items) {
        boolean requiresNormalization(PDCManager pdc, EnchantmentItemSanitizer.ValidityRules rules) {
            for (ItemStack item : items) {
                if (EnchantmentItemSanitizer.requiresNormalization(item, pdc, rules)) {
                    return true;
                }
            }
            return false;
        }
    }
}
