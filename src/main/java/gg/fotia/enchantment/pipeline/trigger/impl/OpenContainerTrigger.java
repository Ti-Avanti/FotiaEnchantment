package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.util.EnumSet;
import java.util.Set;

/**
 * 打开容器触发器 - 玩家打开箱子/木桶/潜影盒等容器时触发
 */
public class OpenContainerTrigger implements Trigger, Listener {

    private static final Set<InventoryType> CONTAINER_TYPES = EnumSet.of(
            InventoryType.CHEST,
            InventoryType.BARREL,
            InventoryType.SHULKER_BOX,
            InventoryType.ENDER_CHEST,
            InventoryType.HOPPER,
            InventoryType.DROPPER,
            InventoryType.DISPENSER
    );

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "OPEN_CONTAINER";
    }

    @Override
    public void register(EffectPipeline pipeline) {
        this.pipeline = pipeline;
        FotiaEnchantment.getInstance().getServer().getPluginManager()
                .registerEvents(this, FotiaEnchantment.getInstance());
    }

    @Override
    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        Inventory inv = event.getInventory();
        if (inv == null || !CONTAINER_TYPES.contains(inv.getType())) {
            return;
        }
        HumanEntity human = event.getPlayer();
        if (!(human instanceof Player player)) {
            return;
        }
        TriggerContext context = TriggerContext.builder()
                .player(player)
                .event(event)
                .item(player.getInventory().getItemInMainHand())
                .value(1)
                .altValue(0)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }
}
