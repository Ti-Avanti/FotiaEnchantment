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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

/**
 * 背刺触发器 - 攻击者在目标背后攻击时触发
 * 通过比较攻击者与目标的朝向判定是否为背刺
 */
public class MeleeAttackBehindTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "MELEE_ATTACK_BEHIND";
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
    public void onBehindAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // 判定攻击者是否在目标背后
        if (!isBehind(player, target)) return;

        double damage = event.getFinalDamage();

        TriggerContext context = TriggerContext.builder()
                .player(player)
                .target(target)
                .event(event)
                .item(player.getInventory().getItemInMainHand())
                .value(damage)
                .altValue(0)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }

    /**
     * 判定攻击者是否在目标背后
     * 通过目标的朝向向量与攻击者相对目标的方向向量进行点积判定
     */
    private boolean isBehind(Player attacker, LivingEntity target) {
        // 目标面朝的方向
        Vector targetDirection = target.getLocation().getDirection().normalize();
        // 攻击者相对于目标的方向
        Vector toAttacker = attacker.getLocation().toVector()
                .subtract(target.getLocation().toVector()).normalize();

        // 点积 > 0 表示攻击者在目标面朝方向的同侧（即目标背后）
        // 使用 0.0 作为阈值，点积为正意味着攻击者在目标背后
        double dot = targetDirection.dot(toAttacker);
        return dot < -0.5; // 攻击者在目标背后（目标面朝方向的反方向）
    }
}
