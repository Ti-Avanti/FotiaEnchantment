package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.entity.LivingEntity;

/**
 * 点燃目标效果 - 设置目标着火时间
 *
 * <p>参数：duration - 着火持续 ticks，默认 80
 */
public class IgniteTargetEffect implements Effect {

    @Override
    public String getId() {
        return "IGNITE_TARGET";
    }

    @Override
    public void execute(EffectContext context) {
        LivingEntity target = context.getTriggerContext().getTarget();
        if (target == null || target.isDead()) return;

        int duration = context.getIntParam("duration", 80);
        if (duration <= 0) return;

        // 取已有着火时间的最大值，避免缩短先前更长的燃烧
        int current = target.getFireTicks();
        target.setFireTicks(Math.max(current, duration));
    }
}
