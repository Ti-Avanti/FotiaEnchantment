package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.compat.BukkitAttributes;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import org.bukkit.entity.LivingEntity;

/**
 * 目标生命值条件 - 目标当前生命百分比位于 [min, max] 之间
 */
public class TargetHealthCondition implements Condition {

    @Override
    public String getId() {
        return "target_health";
    }

    @Override
    public boolean check(ConditionContext context) {
        LivingEntity target = context.getTriggerContext().getTarget();
        if (target == null) {
            return false;
        }
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        if (cfg == null) {
            return false;
        }
        double min = context.evaluateExpression(cfg.getString("min", "0"));
        double max = context.evaluateExpression(cfg.getString("max", "100"));

        double maxHealth = BukkitAttributes.maxHealthValue(target);
        if (maxHealth <= 0) {
            return false;
        }
        double percent = target.getHealth() / maxHealth * 100.0;
        return percent >= min && percent <= max;
    }
}
