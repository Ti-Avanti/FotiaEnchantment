package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import gg.fotia.enchantment.core.EnchantmentData;
import org.bukkit.entity.Player;

/**
 * 着火状态条件 - 玩家是否处于着火状态
 */
public class OnFireCondition implements Condition {

    @Override
    public String getId() {
        return "on_fire";
    }

    @Override
    public boolean check(ConditionContext context) {
        Player player = context.getTriggerContext().getPlayer();
        if (player == null) {
            return false;
        }
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        boolean expected = cfg == null || cfg.getBoolean("value", true);
        boolean burning = player.getFireTicks() > 0;
        return burning == expected;
    }
}
