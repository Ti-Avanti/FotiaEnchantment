package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityUnleashEvent;

/**
 * 解开拴绳触发器
 */
public class UnleashEntityTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "UNLEASH_ENTITY";
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
    public void onUnleash(EntityUnleashEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        // 找到关联的玩家：通过持绳实体（如果是玩家）；
        // EntityUnleashEvent 没有直接 player 字段，因此遍历附近所有玩家更安全
        // 这里只在事件原因是 PLAYER_UNLEASH 时尝试触发
        if (event.getReason() != EntityUnleashEvent.UnleashReason.PLAYER_UNLEASH) {
            return;
        }
        if (living.getLocation().getWorld() == null) {
            return;
        }
        for (Player player : living.getLocation().getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(living.getLocation()) > 36) {
                continue;
            }
            TriggerContext ctx = TriggerContext.builder()
                    .player(player)
                    .event(event)
                    .item(player.getInventory().getItemInMainHand())
                    .target(living)
                    .value(1)
                    .altValue(0)
                    .triggerId(getId())
                    .build();
            pipeline.execute(ctx);
            break;
        }
    }
}
