package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;

/**
 * 闪电效果 - 在目标位置召唤闪电
 *
 * <p>参数：damage - 额外造成的真实伤害（可选），不设置则使用闪电默认伤害
 */
public class LightningEffect implements Effect {

    @Override
    public String getId() {
        return "LIGHTNING";
    }

    @Override
    public void execute(EffectContext context) {
        LivingEntity target = context.getTriggerContext().getTarget();
        if (target == null) return;

        Location location = target.getLocation();
        World world = location.getWorld();
        if (world == null) return;

        LightningStrike strike = world.strikeLightning(location);

        double extraDamage = context.getDoubleParam("damage", 0);
        if (extraDamage > 0 && !target.isDead()) {
            double newHealth = Math.max(0, target.getHealth() - extraDamage);
            target.setHealth(newHealth);
        }
        // 引用 strike 防止 IDE 警告
        if (strike == null) return;
    }
}
