package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.LocationDistance;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * 三叉戟攻击触发器 - 三叉戟命中实体时触发
 */
public class TridentAttackTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "TRIDENT_ATTACK";
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
    public void onTridentAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        double damage = event.getFinalDamage();
        double flyDistance = LocationDistance.safeDistance(trident.getLocation(), player.getLocation());

        TriggerContext context = TriggerContext.builder()
                .player(player)
                .target(target)
                .event(event)
                .item(trident.getItemStack())
                .value(damage)
                .altValue(flyDistance)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }
}
