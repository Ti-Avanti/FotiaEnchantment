package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 近战暴击触发器 - 玩家暴击攻击时触发
 * 暴击条件：玩家正在下落且不在地面上
 */
public class MeleeAttackCriticalTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "MELEE_ATTACK_CRITICAL";
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
    public void onCriticalAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // 暴击判定：玩家正在下落且没有站在实体方块上
        if (isSupported(player)) return;
        if (player.getVelocity().getY() >= 0) return;
        if (player.getFallDistance() <= 0) return;
        // 额外条件：不在水中、不在梯子上、不骑乘、没有失明效果
        if (player.isInWater()) return;
        if (player.isInsideVehicle()) return;
        if (player.isSprinting()) return;

        double damage = event.getFinalDamage();

        TriggerContext context = TriggerContext.builder()
                .player(player)
                .target(target)
                .event(event)
                .item(player.getInventory().getItemInMainHand())
                .value(damage)
                .altValue(1.5) // 暴击倍率
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }

    private boolean isSupported(Player player) {
        Material below = player.getLocation().clone().subtract(0, 0.1, 0).getBlock().getType();
        return below.isSolid();
    }
}
