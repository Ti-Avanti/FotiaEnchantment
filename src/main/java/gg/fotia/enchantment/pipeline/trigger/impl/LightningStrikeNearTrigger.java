package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Location;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.LightningStrikeEvent;

/**
 * 闪电附近触发器 - 闪电劈下时通知 64 格内所有玩家
 */
public class LightningStrikeNearTrigger implements Trigger, Listener {

    private static final double RADIUS = 64.0D;

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "LIGHTNING_STRIKE_NEAR";
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
    public void onLightning(LightningStrikeEvent event) {
        LightningStrike lightning = event.getLightning();
        if (lightning == null) {
            return;
        }
        Location loc = lightning.getLocation();
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        for (Player player : loc.getWorld().getPlayers()) {
            if (player == null) {
                continue;
            }
            double distance = player.getLocation().distance(loc);
            if (distance > RADIUS) {
                continue;
            }
            TriggerContext context = TriggerContext.builder()
                    .player(player)
                    .event(event)
                    .item(player.getInventory().getItemInMainHand())
                    .value(distance)
                    .altValue(0)
                    .triggerId(getId())
                    .build();
            pipeline.execute(context);
        }
    }
}
