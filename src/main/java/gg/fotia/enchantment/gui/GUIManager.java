package gg.fotia.enchantment.gui;

import gg.fotia.enchantment.FotiaEnchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * GUI 生命周期管理器
 * <p>
 * 统一负责自定义 GUI 的事件路由与生存期管理:
 * <ul>
 *     <li>跟踪每个玩家当前打开的 {@link BaseGUI}</li>
 *     <li>拦截所有点击 / 拖拽事件以防止物品被取出</li>
 *     <li>将事件转发到对应 GUI 的处理方法</li>
 * </ul>
 */
public class GUIManager implements Listener {

    private final FotiaEnchantment plugin;
    private final Map<UUID, BaseGUI> openGUIs = new HashMap<>();

    public GUIManager(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开一个 GUI 并记录追踪
     */
    public void open(BaseGUI gui) {
        if (gui == null) return;
        openGUIs.put(gui.getPlayer().getUniqueId(), gui);
        gui.open();
    }

    /**
     * 获取玩家当前打开的 GUI
     */
    public BaseGUI getOpen(Player player) {
        if (player == null) return null;
        return openGUIs.get(player.getUniqueId());
    }

    /**
     * 关闭并清除追踪 (不调用客户端关闭)
     */
    public void clear(Player player) {
        if (player == null) return;
        openGUIs.remove(player.getUniqueId());
    }

    // ============================================
    // 事件处理
    // ============================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        BaseGUI gui = openGUIs.get(player.getUniqueId());
        if (gui == null) return;

        Inventory clicked = event.getClickedInventory();
        Inventory top = event.getView().getTopInventory();

        // 防止跨容器 shift 点击导致物品移入 GUI
        if (clicked != null && clicked.equals(top)) {
            // 默认禁止从 GUI 中拿出物品
            event.setCancelled(true);
        } else {
            // 在玩家自己背包点击时, 禁止 shift+click 移入 GUI
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
        }

        gui.handleClick(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        BaseGUI gui = openGUIs.get(player.getUniqueId());
        if (gui == null) return;

        Inventory top = event.getView().getTopInventory();
        // 若拖拽涉及 GUI 顶层, 一律取消
        for (int slot : event.getRawSlots()) {
            if (slot < top.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        BaseGUI gui = openGUIs.remove(player.getUniqueId());
        if (gui == null) return;
        try {
            gui.handleClose(event);
        } catch (Throwable t) {
            plugin.getLogger().warning("处理 GUI 关闭事件出错: " + t.getMessage());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        openGUIs.remove(event.getPlayer().getUniqueId());
    }
}
