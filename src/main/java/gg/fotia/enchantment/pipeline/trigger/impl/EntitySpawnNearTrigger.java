package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.LocationDistance;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

/**
 * 实体在附近生成触发器
 */
public class EntitySpawnNearTrigger implements Trigger, Listener {

    private static final double RADIUS = 16.0;

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "ENTITY_SPAWN_NEAR";
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
    public void onSpawn(EntitySpawnEvent event) {
        Location loc = event.getLocation();
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        for (Player player : loc.getWorld().getPlayers()) {
            if (LocationDistance.safeDistanceSquared(player.getLocation(), loc) > RADIUS * RADIUS) {
                continue;
            }
            TriggerContext.Builder builder = TriggerContext.builder()
                    .player(player)
                    .event(event)
                    .item(player.getInventory().getItemInMainHand())
                    .value(1)
                    .altValue(0)
                    .triggerId(getId());
            if (event.getEntity() instanceof LivingEntity living) {
                builder.target(living);
            }
            pipeline.execute(builder.build());
        }
    }
}
