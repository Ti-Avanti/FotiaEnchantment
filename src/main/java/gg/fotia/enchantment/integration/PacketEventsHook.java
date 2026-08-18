package gg.fotia.enchantment.integration;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetCursorItem;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.compat.BukkitItemFlags;
import gg.fotia.enchantment.compat.BukkitRegistryCompat;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.EnchantmentItemSanitizer;
import gg.fotia.enchantment.core.EnchantmentRegistry;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.lore.item.EnchantmentGeneratedLoreStripper;
import gg.fotia.enchantment.lore.item.EnchantmentLoreCleaner;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * PacketEvents 集成钩子
 * <p>
 * 通过拦截发送给玩家的 SET_SLOT / WINDOW_ITEMS 包,
 * 在含有自定义附魔的物品 lore 上前置追加 "稀有度颜色 + 附魔名 + 等级" 行。
 * 玩家看到的 lore 会随其客户端语言而变化, 但物品本身的 NBT 不会被修改。
 */
public class PacketEventsHook {

    private final FotiaEnchantment plugin;
    private boolean available;
    private final NamespacedKey guiGlowKey;
    private PacketListenerAbstract listener;

    public PacketEventsHook(FotiaEnchantment plugin) {
        this.plugin = plugin;
        this.guiGlowKey = new NamespacedKey(plugin, "gui_glow");
    }

    /**
     * 初始化 PacketEvents 监听
     */
    public void init() {
        try {
            if (Bukkit.getPluginManager().getPlugin("packetevents") == null) {
                available = false;
                return;
            }
            listener = new ItemPacketListener();
            PacketEvents.getAPI().getEventManager()
                    .registerListener(listener);
            available = true;
            plugin.getLogger().info("PacketEvents 集成已启用 (附魔 lore 注入)");
        } catch (Throwable t) {
            available = false;
            plugin.getLogger().log(Level.WARNING, "PacketEvents 集成初始化失败", t);
        }
    }

    /**
     * 关闭并注销监听
     */
    public void shutdown() {
        if (!available || listener == null) return;
        try {
            PacketEvents.getAPI().getEventManager().unregisterListener(listener);
        } catch (Throwable ignored) {
        }
    }

    public boolean isAvailable() {
        return available;
    }

    static boolean shouldDecorateItemPacket(PacketTypeCommon packetType) {
        return packetType == PacketType.Play.Server.SET_SLOT
                || packetType == PacketType.Play.Server.WINDOW_ITEMS
                || packetType == PacketType.Play.Server.SET_CURSOR_ITEM
                || packetType == PacketType.Play.Server.SET_PLAYER_INVENTORY;
    }

    static boolean shouldDecorateForGameMode(GameMode gameMode) {
        return gameMode != GameMode.CREATIVE && gameMode != GameMode.SPECTATOR;
    }

    /**
     * 物品包监听器
     */
    private class ItemPacketListener extends PacketListenerAbstract {
        private ItemPacketListener() {
            super(PacketListenerPriority.NORMAL);
        }

        @Override
        public void onPacketSend(PacketSendEvent event) {
            try {
                PacketTypeCommon packetType = event.getPacketType();
                if (!shouldDecorateItemPacket(packetType)) {
                    return;
                }
                if (packetType == PacketType.Play.Server.SET_SLOT) {
                    handleSetSlot(event);
                } else if (packetType == PacketType.Play.Server.WINDOW_ITEMS) {
                    handleWindowItems(event);
                } else if (packetType == PacketType.Play.Server.SET_CURSOR_ITEM) {
                    handleSetCursorItem(event);
                } else if (packetType == PacketType.Play.Server.SET_PLAYER_INVENTORY) {
                    handleSetPlayerInventory(event);
                }
            } catch (Throwable t) {
                // 任何异常都不阻断包发送
                plugin.getLogger().log(Level.FINE, "PacketEvents 处理物品包出错", t);
            }
        }

        private void handleSetSlot(PacketSendEvent event) {
            WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
            Player player = resolvePlayer(event.getUser());
            if (player == null || !shouldDecorateForGameMode(player.getGameMode())) return;

            ItemStack bukkit = SpigotConversionUtil.toBukkitItemStack(wrapper.getItem());
            ItemStack modified = decorate(player, bukkit);
            if (modified != null) {
                wrapper.setItem(SpigotConversionUtil.fromBukkitItemStack(modified));
                event.markForReEncode(true);
            }
        }

        private void handleWindowItems(PacketSendEvent event) {
            WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
            Player player = resolvePlayer(event.getUser());
            if (player == null || !shouldDecorateForGameMode(player.getGameMode())) return;

            List<com.github.retrooper.packetevents.protocol.item.ItemStack> items = wrapper.getItems();
            boolean any = false;
            List<com.github.retrooper.packetevents.protocol.item.ItemStack> result = new ArrayList<>(items.size());
            for (com.github.retrooper.packetevents.protocol.item.ItemStack peItem : items) {
                ItemStack bukkit = SpigotConversionUtil.toBukkitItemStack(peItem);
                ItemStack modified = decorate(player, bukkit);
                if (modified != null) {
                    result.add(SpigotConversionUtil.fromBukkitItemStack(modified));
                    any = true;
                } else {
                    result.add(peItem);
                }
            }

            com.github.retrooper.packetevents.protocol.item.ItemStack carried = wrapper.getCarriedItem().orElse(null);
            if (carried != null) {
                ItemStack bukkit = SpigotConversionUtil.toBukkitItemStack(carried);
                ItemStack modified = decorate(player, bukkit);
                if (modified != null) {
                    wrapper.setCarriedItem(SpigotConversionUtil.fromBukkitItemStack(modified));
                    any = true;
                }
            }

            if (any) {
                wrapper.setItems(result);
                event.markForReEncode(true);
            }
        }

        private void handleSetCursorItem(PacketSendEvent event) {
            WrapperPlayServerSetCursorItem wrapper = new WrapperPlayServerSetCursorItem(event);
            Player player = resolvePlayer(event.getUser());
            if (player == null || !shouldDecorateForGameMode(player.getGameMode())) return;

            ItemStack bukkit = SpigotConversionUtil.toBukkitItemStack(wrapper.getStack());
            ItemStack modified = decorate(player, bukkit);
            if (modified != null) {
                wrapper.setStack(SpigotConversionUtil.fromBukkitItemStack(modified));
                event.markForReEncode(true);
            }
        }

        private void handleSetPlayerInventory(PacketSendEvent event) {
            WrapperPlayServerSetPlayerInventory wrapper = new WrapperPlayServerSetPlayerInventory(event);
            Player player = resolvePlayer(event.getUser());
            if (player == null || !shouldDecorateForGameMode(player.getGameMode())) return;

            ItemStack bukkit = SpigotConversionUtil.toBukkitItemStack(wrapper.getStack());
            ItemStack modified = decorate(player, bukkit);
            if (modified != null) {
                wrapper.setStack(SpigotConversionUtil.fromBukkitItemStack(modified));
                event.markForReEncode(true);
            }
        }
    }

    /**
     * 根据玩家语言完全接管物品附魔 lore，不修改原物品。
     *
     * @return 含修改后元数据的 ItemStack 副本; 无需修改时返回 null
     */
    private ItemStack decorate(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        if (!item.hasItemMeta()) return null;

        EnchantmentManager enchantManager = plugin.getEnchantmentManager();
        if (enchantManager == null) return null;
        PDCManager pdc = enchantManager.getPdcManager();

        ItemMeta sourceMeta = item.getItemMeta();
        if (sourceMeta == null) return null;

        List<LoreEntry> entries = collectLoreEntries(item, sourceMeta, enchantManager, pdc, false);
        List<LoreEntry> sourceEntries = collectLoreEntries(item, sourceMeta, enchantManager, pdc, true);
        // 无任何相关附魔的物品(绝大多数)在克隆与 lore 计算之前直接返回
        if (entries.isEmpty() && sourceEntries.isEmpty()) return null;

        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) return null;

        List<Component> existingLore = EnchantmentLoreCleaner.stripAllGeneratedLore(
                plugin,
                item,
                meta,
                meta.lore());
        List<Component> generatedLore = EnchantmentLoreCleaner.localizedGeneratedLore(
                plugin,
                player,
                item,
                false,
                false);
        List<Component> sourceGeneratedLore = EnchantmentLoreCleaner.localizedGeneratedLore(
                plugin,
                player,
                item,
                true,
                true);
        if (generatedLore.isEmpty() && sourceGeneratedLore.isEmpty()) return null;

        BukkitItemFlags.hideEnchantments(meta);
        List<Component> mergedLore = EnchantmentLoreCleaner.mergeGeneratedLore(existingLore, generatedLore, sourceGeneratedLore);
        if (generatedLore.isEmpty() && mergedLore.equals(existingLore)) return null;
        meta.lore(mergedLore.isEmpty() ? null : mergedLore);
        copy.setItemMeta(meta);
        return copy;
    }

    private List<LoreEntry> collectLoreEntries(ItemStack item,
                                               ItemMeta meta,
                                               EnchantmentManager enchantManager,
                                               PDCManager pdc,
                                               boolean includeDisabledVanilla) {
        Map<String, LoreEntry> entries = new LinkedHashMap<>();

        for (Map.Entry<String, Integer> entry : pdc.getEnchantments(meta).entrySet()) {
            String id = normalizeId(entry.getKey());
            int level = entry.getValue();
            if (id.isEmpty() || level <= 0) {
                continue;
            }
            EnchantmentData data = enchantManager.getEnchantment(id);
            if (!EnchantmentItemSanitizer.isValid(data, item.getType(), level)) {
                continue;
            }
            entries.put("custom:" + id, new LoreEntry(id, level, true, null, data));
        }

        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            if (isSyntheticGuiGlow(meta, entry.getKey())) {
                continue;
            }
            addVanillaEntry(entries, entry.getKey(), entry.getValue(), includeDisabledVanilla);
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<Enchantment, Integer> entry : storageMeta.getStoredEnchants().entrySet()) {
                if (isSyntheticGuiGlow(meta, entry.getKey())) {
                    continue;
                }
                addVanillaEntry(entries, entry.getKey(), entry.getValue(), includeDisabledVanilla);
            }
        }

        return new ArrayList<>(entries.values());
    }

    private boolean isSyntheticGuiGlow(ItemMeta meta, Enchantment enchantment) {
        Enchantment unbreaking = BukkitRegistryCompat.unbreakingEnchantment();
        return enchantment != null
                && enchantment.equals(unbreaking)
                && meta.getPersistentDataContainer().has(guiGlowKey, PersistentDataType.BYTE);
    }

    private void addVanillaEntry(Map<String, LoreEntry> entries,
                                 Enchantment enchantment,
                                 int level,
                                 boolean includeDisabledVanilla) {
        if (enchantment == null || level <= 0) {
            return;
        }
        NamespacedKey key = enchantment.getKey();
        if (key == null || EnchantmentRegistry.getNamespace().equals(key.getNamespace())) {
            return;
        }
        if (!"minecraft".equals(key.getNamespace())) {
            return;
        }
        if (!includeDisabledVanilla && isDisabledVanilla(enchantment)) {
            return;
        }
        String id = normalizeId(key.getKey());
        entries.putIfAbsent("vanilla:" + id, new LoreEntry(id, level, false, enchantment, null));
    }

    private boolean isDisabledVanilla(Enchantment enchantment) {
        if (plugin.getVanillaManager() == null || enchantment == null || enchantment.getKey() == null) {
            return false;
        }
        NamespacedKey key = enchantment.getKey();
        if (!"minecraft".equals(key.getNamespace())) {
            return false;
        }
        return plugin.getVanillaManager().isDisabled(enchantment);
    }

    static List<Component> stripGeneratedLoreCopies(List<Component> existingLore, List<Component> generatedLore) {
        return EnchantmentGeneratedLoreStripper.stripGeneratedLoreCopies(existingLore, generatedLore);
    }

    private String normalizeId(String id) {
        return id == null ? "" : id.toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * 通过 PacketEvents User 获取 Bukkit 玩家
     */
    private Player resolvePlayer(User user) {
        if (user == null) return null;
        UUID uuid = user.getUUID();
        if (uuid == null) return null;
        return Bukkit.getPlayer(uuid);
    }

    private record LoreEntry(String id,
                             int level,
                             boolean custom,
                             Enchantment enchantment,
                             EnchantmentData data) {
    }
}
