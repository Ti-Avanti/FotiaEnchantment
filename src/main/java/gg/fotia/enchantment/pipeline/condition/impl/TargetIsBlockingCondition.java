package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import gg.fotia.enchantment.core.EnchantmentData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 目标在格挡条件 - 目标玩家是否举盾格挡
 */
public class TargetIsBlockingCondition implements Condition {

    @Override
    public String getId() {
        return "target_is_blocking";
    }

    @Override
    public boolean check(ConditionContext context) {
        LivingEntity target = context.getTriggerContext().getTarget();
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        boolean expected = cfg == null || cfg.getBoolean("value", true);
        boolean blocking = target instanceof Player p && p.isBlocking();
        return blocking == expected;
    }
}
