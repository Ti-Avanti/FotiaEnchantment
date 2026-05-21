package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * 爆头触发器 - 投射物命中目标头部时触发
 * 通过检查投射物命中位置是否在目标眼睛位置附近判定
 */
public class HeadshotTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    /** 头部判定容差（方块） */
    private static final double HEAD_TOLERANCE = 0.6;

    @Override
    public String getId() {
        return "HEADSHOT";
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
    public void onHeadshot(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile projectile)) return;
        if (!(projectile.getShooter() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // 检查命中位置是否在头部附近
        double projectileY = projectile.getLocation().getY();
        double eyeY = target.getEyeLocation().getY();

        if (Math.abs(projectileY - eyeY) > HEAD_TOLERANCE) return;

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
}
