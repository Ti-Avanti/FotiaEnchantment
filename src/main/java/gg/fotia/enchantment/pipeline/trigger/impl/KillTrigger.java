package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * 击杀触发器 - 玩家击杀任何生物时触发
 */
public class KillTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "KILL";
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
    public void onKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        double maxHealth = 20;
        if (entity.getAttribute(Attribute.MAX_HEALTH) != null) {
            maxHealth = entity.getAttribute(Attribute.MAX_HEALTH).getValue();
        }

        TriggerContext context = TriggerContext.builder()
                .player(killer)
                .target(entity)
                .event(event)
                .item(killer.getInventory().getItemInMainHand())
                .value(1)
                .altValue(maxHealth)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }
}
