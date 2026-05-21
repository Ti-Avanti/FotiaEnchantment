package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.gui.CodexGUI;
import gg.fotia.enchantment.gui.DisenchantGUI;
import gg.fotia.enchantment.gui.FragmentCraftGUI;
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

/**
 * 道具使用监听器
 * 监听 PlayerInteractEvent（右键）:
 * - 右键星辉残页 → 打开合成 GUI
 * - 右键星芒魔典 → 打开揭示 GUI
 * - 右键祛魔之石 → 打开祛魔 GUI
 */
public class ItemUseListener implements Listener {

    private final FotiaEnchantment plugin;
    private final CustomItemManager customItemManager;

    public ItemUseListener(FotiaEnchantment plugin) {
        this.plugin = plugin;
        this.customItemManager = plugin.getCustomItemManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 只处理右键
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // 避免副手重复触发
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

        // 根据物品类型路由事件
        switch (itemType) {
            case "starweave-fragment" -> {
                event.setCancelled(true);
                plugin.getGuiManager().open(new FragmentCraftGUI(plugin, player));
            }
            case "stellaris-codex" -> {
                event.setCancelled(true);
                handleCodexUse(player, item);
            }
            case "disenchant-shard", "disenchant-crystal", "disenchant-gem" -> {
                event.setCancelled(true);
                handleDisenchantStoneUse(player, item, itemType);
            }
            default -> {
                // 其他自定义道具不做处理
            }
        }
    }

    /**
     * 处理星芒魔典使用。
     */
    private void handleCodexUse(Player player, ItemStack codex) {
        ItemStack singleCodex = codex.clone();
        singleCodex.setAmount(1);
        codex.setAmount(codex.getAmount() - 1);
        plugin.getGuiManager().open(new CodexGUI(plugin, player, singleCodex));
    }

    /**
     * 处理祛魔之石使用。
     */
    private void handleDisenchantStoneUse(Player player, ItemStack stone, String itemType) {
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
}
