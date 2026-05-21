package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * 着陆触发器 - 玩家从空中落到地面
 */
public class LandTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "LAND";
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
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        // 不区分方块时跳过
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        // 只关心从非地面 -> 地面，避免依赖废弃的地面状态 API。
        if (!isSupported(to)) {
            return;
        }
        float fallDistance = player.getFallDistance();
        if (fallDistance <= 0.5f) {
            return;
        }
        TriggerContext context = TriggerContext.builder()
                .player(player)
                .event(event)
                .item(player.getInventory().getItemInMainHand())
                .value(fallDistance)
                .altValue(0)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }

    private boolean isSupported(Location location) {
        Material below = location.clone().subtract(0, 0.1, 0).getBlock().getType();
        return below.isSolid();
    }
}
