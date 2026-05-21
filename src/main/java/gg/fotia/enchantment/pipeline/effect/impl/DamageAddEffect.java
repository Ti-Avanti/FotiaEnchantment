package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 固定伤害加成效果 - 给事件增加固定伤害
 */
public class DamageAddEffect implements Effect {

    @Override
    public String getId() {
        return "DAMAGE_ADD";
    }

    @Override
    public void execute(EffectContext context) {
        Event event = context.getTriggerContext().getEvent();
        if (!(event instanceof EntityDamageEvent damageEvent)) return;

        String valueStr = context.getConfigValue();
        if (valueStr == null || valueStr.isEmpty()) return;

        double bonus = context.evaluateExpression(valueStr);
        damageEvent.setDamage(damageEvent.getDamage() + bonus);
    }
}
