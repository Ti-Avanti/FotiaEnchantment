package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 减伤效果 - 按比例减少受到的伤害
 *
 * <p>value 取值范围 0-100，表示减伤百分比。
 * 例如 value=25 表示减少 25% 的伤害。
 */
public class DamageReduceEffect implements Effect {

    @Override
    public String getId() {
        return "DAMAGE_REDUCE";
    }

    @Override
    public void execute(EffectContext context) {
        Event event = context.getTriggerContext().getEvent();
        if (!(event instanceof EntityDamageEvent damageEvent)) return;

        String valueStr = context.getConfigValue();
        if (valueStr == null || valueStr.isEmpty()) return;

        double percent = context.evaluateExpression(valueStr);
        if (percent <= 0) return;
        if (percent > 100) percent = 100;

        double reducedDamage = damageEvent.getDamage() * (1.0 - percent / 100.0);
        damageEvent.setDamage(Math.max(0, reducedDamage));
    }
}
