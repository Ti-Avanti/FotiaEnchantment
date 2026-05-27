package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.compat.BukkitAttributes;
import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import org.bukkit.entity.Player;

/**
 * 生命值低于条件 - 玩家当前生命百分比低于指定阈值
 */
public class HealthBelowCondition implements Condition {

    @Override
    public String getId() {
        return "health_below";
    }

    @Override
    public boolean check(ConditionContext context) {
        Player player = context.getTriggerContext().getPlayer();
        if (player == null) {
            return false;
        }
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        if (cfg == null) {
            return false;
        }
        String valueStr = cfg.getString("value");
        if (valueStr == null || valueStr.isEmpty()) {
            return false;
        }
        double threshold = context.evaluateExpression(valueStr);

        double max = BukkitAttributes.maxHealthValue(player);
        if (max <= 0) {
            return false;
        }
        double percent = player.getHealth() / max * 100.0;
        return percent < threshold;
    }
}
