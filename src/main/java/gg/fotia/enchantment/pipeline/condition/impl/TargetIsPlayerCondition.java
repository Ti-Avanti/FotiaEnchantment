package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import gg.fotia.enchantment.core.EnchantmentData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 目标是玩家条件
 */
public class TargetIsPlayerCondition implements Condition {

    @Override
    public String getId() {
        return "target_is_player";
    }

    @Override
    public boolean check(ConditionContext context) {
        LivingEntity target = context.getTriggerContext().getTarget();
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        boolean expected = cfg == null || cfg.getBoolean("value", true);
        boolean isPlayer = target instanceof Player;
        return isPlayer == expected;
    }
}
