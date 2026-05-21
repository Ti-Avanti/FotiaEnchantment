package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 伤害倍增效果 - 将当前事件伤害乘以指定倍率
 */
public class DamageMultiplyEffect implements Effect {

    @Override
    public String getId() {
        return "DAMAGE_MULTIPLY";
    }

    @Override
    public void execute(EffectContext context) {
        Event event = context.getTriggerContext().getEvent();
        if (!(event instanceof EntityDamageEvent damageEvent)) return;

        String valueStr = context.getConfigValue();
        if (valueStr == null || valueStr.isEmpty()) return;

        double multiplier = context.evaluateExpression(valueStr);
        if (multiplier <= 0) return;

        double newDamage = damageEvent.getDamage() * multiplier;
        damageEvent.setDamage(newDamage);
    }
}
