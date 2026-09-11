package gg.fotia.enchantment.gui;

import gg.fotia.enchantment.FotiaEnchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
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
    private final Map<UUID, BaseGUI> openGUIs = new ConcurrentHashMap<>();

    public GUIManager(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开一个 GUI 并记录追踪
     */
    public void open(BaseGUI gui) {
        if (gui == null) return;
        gui.open();
        openGUIs.put(gui.getPlayer().getUniqueId(), gui);
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

    /**
     * 插件禁用/重载时强制关闭所有追踪中的 GUI。
     * 先触发各 GUI 的关闭逻辑归还暂存物品(祛魔石、装备、魔典等), 再关闭客户端界面,
     * 避免监听器注销后玩家关闭界面时物品永久丢失。
     */
    public void shutdown() {
        if (openGUIs.isEmpty()) return;
        List<BaseGUI> guis = new ArrayList<>(openGUIs.values());
        // 先清空追踪, 避免 closeInventory 触发的 InventoryCloseEvent 二次处理
        openGUIs.clear();
        for (BaseGUI gui : guis) {
            closeGui(gui);
        }
    }

    private void closeGui(BaseGUI gui) {
        try {
            gui.handleClose(null);
            Player player = gui.getPlayer();
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("插件禁用时归还 GUI 物品出错: " + t.getMessage());
        }
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

        // 双击收集(COLLECT_TO_CURSOR)会跨容器吸取与光标匹配的可堆叠物品,
        // 包括 GUI 顶层展示槽中的真实物品, 必须无条件取消以防刷物品
        if (event.getClick() == ClickType.DOUBLE_CLICK
                || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
        } else if (clicked != null && clicked.equals(top)) {
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
        BaseGUI gui = openGUIs.get(player.getUniqueId());
        if (gui == null) return;
        if (!isTrackedInventoryClose(gui.getInventory(), event.getInventory())) {
            return;
        }
        if (!openGUIs.remove(player.getUniqueId(), gui)) {
            return;
        }
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

    static boolean isTrackedInventoryClose(Object trackedInventory, Object closedInventory) {
        return trackedInventory != null && trackedInventory.equals(closedInventory);
    }
}
