package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.entity.LivingEntity;

/**
 * 真实伤害效果 - 对目标造成无视护甲的伤害
 */
public class TrueDamageEffect implements Effect {

    @Override
    public String getId() {
        return "TRUE_DAMAGE";
    }

    @Override
    public void execute(EffectContext context) {
        LivingEntity target = context.getTriggerContext().getTarget();
        if (target == null || target.isDead()) return;

        String valueStr = context.getConfigValue();
        if (valueStr == null || valueStr.isEmpty()) return;

        double damage = context.evaluateExpression(valueStr);
        if (damage <= 0) return;

        // 真实伤害：直接减少生命值，无视护甲与抗性
        double newHealth = Math.max(0, target.getHealth() - damage);
        target.setHealth(newHealth);
    }
}
