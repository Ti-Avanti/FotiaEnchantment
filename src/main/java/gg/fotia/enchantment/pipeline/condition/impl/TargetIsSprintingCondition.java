package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import gg.fotia.enchantment.core.EnchantmentData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 目标在冲刺条件
 */
public class TargetIsSprintingCondition implements Condition {

    @Override
    public String getId() {
        return "target_is_sprinting";
    }

    @Override
    public boolean check(ConditionContext context) {
        LivingEntity target = context.getTriggerContext().getTarget();
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        boolean expected = cfg == null || cfg.getBoolean("value", true);
        boolean sprinting = target instanceof Player p && p.isSprinting();
        return sprinting == expected;
    }
}
