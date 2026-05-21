package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 反伤效果 - 对攻击者造成所受伤害的一定比例
 *
 * <p>value 取值范围 0-100+，表示反伤百分比。
 * 例如 value=50 表示反弹 50% 受到的伤害给攻击者。
 */
public class ThornsEffect implements Effect {

    @Override
    public String getId() {
        return "THORNS";
    }

    @Override
    public void execute(EffectContext context) {
        Event event = context.getTriggerContext().getEvent();
        if (!(event instanceof EntityDamageByEntityEvent damageEvent)) return;
        if (!(damageEvent.getDamager() instanceof LivingEntity attacker)) return;

        String valueStr = context.getConfigValue();
        if (valueStr == null || valueStr.isEmpty()) return;

        double percent = context.evaluateExpression(valueStr);
        if (percent <= 0) return;

        double reflected = damageEvent.getDamage() * (percent / 100.0);
        if (reflected <= 0) return;

        Player owner = context.getTriggerContext().getPlayer();
        if (owner != null && !attacker.equals(owner)) {
            attacker.damage(reflected, owner);
        } else {
            attacker.damage(reflected);
        }
    }
}
