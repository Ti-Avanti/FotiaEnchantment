package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.compat.BukkitItemFlags;
import gg.fotia.enchantment.core.EnchantmentItemSanitizer;
import gg.fotia.enchantment.core.EnchantmentLimitPolicy;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.core.VanillaManager;
import gg.fotia.enchantment.lore.item.EnchantmentDisplayPolicy;
import gg.fotia.enchantment.lore.item.EnchantmentLoreCleaner;
import gg.fotia.enchantment.util.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EnchantmentDisplayListener implements Listener {

    private final FotiaEnchantment plugin;
    private final Set<UUID> pendingNormalizations = ConcurrentHashMap.newKeySet();
    private final Queue<UUID> validityScanQueue = new ConcurrentLinkedQueue<>();
    private volatile EnchantmentItemSanitizer.ValidityRules cachedValidityRules;
    private Object validityScanTask;

    public EnchantmentDisplayListener(FotiaEnchantment plugin) {
        this.plugin = plugin;
        refreshValidityRules();
        restartValidityScan();
    }

    public void reload() {
        refreshValidityRules();
        validityScanQueue.clear();
        restartValidityScan();
    }

    public void shutdown() {
        SchedulerUtils.cancelTask(validityScanTask);
        validityScanTask = null;
        validityScanQueue.clear();
        pendingNormalizations.clear();
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
            scheduleNormalize(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event instanceof InventoryCreativeEvent) {
            return;
        }
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
        scheduleNormalizeMechanicTopInventory(event.getEnchanter(), event.getView().getTopInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        // 仅发放/修改物品类命令需要立即规范化, 其余命令交给周期巡检兜底
        if (isItemGrantingCommand(event.getMessage())) {
            scheduleNormalize(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        if (isItemGrantingCommand(event.getCommand())) {
            SchedulerUtils.runTask(plugin, this::normalizeOnlinePlayers);
        }
    }

    /**
     * 判断命令是否可能给玩家发放/修改物品
     */
    static boolean isItemGrantingCommand(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        String command = message.charAt(0) == '/' ? message.substring(1) : message;
        int space = command.indexOf(' ');
        String label = (space > 0 ? command.substring(0, space) : command).toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0 && colon < label.length() - 1) {
            label = label.substring(colon + 1);
        }
        return switch (label) {
            case "give", "item", "i", "giveitem", "fe", "enchant", "clear" -> true;
            default -> false;
        };
    }

    private void scheduleNormalize(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (!pendingNormalizations.add(playerId)) {
            return;
        }
        // Folia 下实体在任务执行前被移除时走 retired 回调, 避免 pending 标记永久滞留
        Object task = SchedulerUtils.runEntityTask(plugin, player, () -> {
            try {
                normalizePlayer(player);
            } finally {
                pendingNormalizations.remove(playerId);
            }
        }, () -> pendingNormalizations.remove(playerId));
        if (task == null) {
            pendingNormalizations.remove(playerId);
        }
    }

    private void scheduleNormalizeMechanicTopInventory(Player player, Inventory inventory) {
        if (player == null) {
            return;
        }
        SchedulerUtils.runEntityTask(plugin, player, () -> {
            if (player == null
                    || !player.isOnline()
                    || inventory == null
                    || !shouldNormalizeMechanicTopInventory(inventory.getType().name())) {
                return;
            }
            if (normalizeInventory(player, inventory, validityRules())) {
                player.updateInventory();
            }
        });
    }

    static boolean shouldNormalizeMechanicTopInventory(String inventoryTypeName) {
        return "ENCHANTING".equals(inventoryTypeName);
    }

    private void normalizeOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            scheduleNormalize(player);
        }
    }

    private void restartValidityScan() {
        SchedulerUtils.cancelTask(validityScanTask);
        validityScanTask = SchedulerUtils.runTaskTimer(
                plugin, this::scheduleValidityBatch, 20L, itemValidityCheckInterval());
    }

    private void scheduleValidityBatch() {
        if (validityScanQueue.isEmpty()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                validityScanQueue.offer(player.getUniqueId());
            }
        }

        int batchSize = plugin.getConfigManager().getItemValidityCheckPlayersPerRun();
        for (int processed = 0; processed < batchSize; processed++) {
            UUID playerId = validityScanQueue.poll();
            if (playerId == null) {
                return;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                scheduleNormalize(player);
            }
        }
    }

    private void normalizePlayer(Player player) {
        if (player == null || !player.isOnline() || shouldSkipInventoryNormalization(player)) {
            return;
        }

        EnchantmentItemSanitizer.ValidityRules rules = validityRules();
        boolean changed = normalizeInventory(player, player.getInventory(), rules);
        ItemStack cursor = player.getItemOnCursor();
        if (normalizeItem(player, cursor, rules)) {
            player.setItemOnCursor(cursor);
            changed = true;
        }

        if (changed) {
            player.updateInventory();
        }
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
        // 廉价预筛: 无附魔无 lore 且不可能需要槽位行的物品(背包中绝大多数)直接跳过
        if (!isNormalizationRelevant(item, meta, pdc)) {
            return false;
        }

        boolean needsSanitization = EnchantmentItemSanitizer.needsSanitization(item, pdc, rules);
        VanillaManager vanillaManager = plugin.getVanillaManager();
        boolean hasDisabled = vanillaManager != null && vanillaManager.hasDisabledEnchantments(item);
        // 仅当确实要修改附魔时才克隆原件(供 FromSource 清理旧 lore)
        ItemStack source = (needsSanitization || hasDisabled) ? item.clone() : null;

        boolean changed = needsSanitization && EnchantmentLoreCleaner.stripGeneratedLore(plugin, player, item);
        changed |= needsSanitization && EnchantmentItemSanitizer.sanitize(plugin, item, rules);
        changed |= hasDisabled && vanillaManager.removeDisabledEnchantments(item);

        meta = item.getItemMeta();
        if (meta == null) {
            return changed;
        }

        if (changed && source != null) {
            changed |= EnchantmentLoreCleaner.applyGeneratedLoreFromSource(plugin, player, item, source);
        } else {
            changed |= EnchantmentLoreCleaner.applyGeneratedLore(plugin, player, item);
        }
        meta = item.getItemMeta();
        if (meta == null) {
            return changed;
        }

        boolean hasStoredEnchants = meta instanceof EnchantmentStorageMeta storageMeta
                && !storageMeta.getStoredEnchants().isEmpty();
        boolean hasLegacyCustomEnchants = !pdc.getLegacyEnchantments(meta).isEmpty();
        if (!EnchantmentDisplayPolicy.shouldHideNativeEnchantments(
                meta.hasEnchants(),
                hasStoredEnchants,
                hasLegacyCustomEnchants)) {
            return changed;
        }

        if (!BukkitItemFlags.hasHideEnchantments(meta)) {
            BukkitItemFlags.hideEnchantments(meta);
            changed = true;
        }
        if (!BukkitItemFlags.hasHideStoredEnchantments(meta)
                && BukkitItemFlags.addHideStoredEnchantments(meta)) {
            changed = true;
        }

        if (changed) {
            item.setItemMeta(meta);
        }
        return changed;
    }

    /**
     * 判断物品是否可能需要任何规范化处理。
     * 有附魔/存储附魔/自定义附魔/lore 的物品需要; 其余仅当
     * 物品不可堆叠且属于可附魔材质(可能需要生成空槽位行)时需要。
     */
    private boolean isNormalizationRelevant(ItemStack item, ItemMeta meta, PDCManager pdc) {
        if (meta.hasEnchants() || meta.hasLore()) {
            return true;
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta && !storageMeta.getStoredEnchants().isEmpty()) {
            return true;
        }
        if (!pdc.getLegacyEnchantments(meta).isEmpty()) {
            return true;
        }
        // 无附魔无 lore: 仅当可能显示空槽位行时才需要处理 (对齐 shouldDisplayEnchantSlotLore)
        if (item.getMaxStackSize() > 1) {
            return false;
        }
        EnchantmentManager manager = plugin.getEnchantmentManager();
        return EnchantmentLimitPolicy.hasKnownItemGroup(item.getType())
                || (manager != null && !manager.getApplicable(item.getType()).isEmpty());
    }

    private PDCManager pdcManager() {
        EnchantmentManager manager = plugin.getEnchantmentManager();
        return manager == null ? null : manager.getPdcManager();
    }

    private EnchantmentItemSanitizer.ValidityRules validityRules() {
        EnchantmentItemSanitizer.ValidityRules rules = cachedValidityRules;
        if (rules == null) {
            refreshValidityRules();
            rules = cachedValidityRules;
        }
        return rules;
    }

    private void refreshValidityRules() {
        EnchantmentManager manager = plugin.getEnchantmentManager();
        cachedValidityRules = manager == null
                ? EnchantmentItemSanitizer.ValidityRules.from(List.of())
                : EnchantmentItemSanitizer.ValidityRules.from(manager.getAllEnchantments());
    }

    private long itemValidityCheckInterval() {
        if (plugin.getConfigManager() == null) {
            return 40L;
        }
        return Math.max(20L, plugin.getConfigManager().getItemValidityCheckInterval());
    }

    private boolean shouldSkipInventoryNormalization(Player player) {
        if (player == null) {
            return true;
        }
        GameMode gameMode = player.getGameMode();
        return gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR;
    }

}
