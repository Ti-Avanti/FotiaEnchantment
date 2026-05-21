package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import org.bukkit.entity.Player;

/**
 * 饥饿度条件 - 玩家饥饿度低于指定值
 */
public class FoodLevelCondition implements Condition {

    @Override
    public String getId() {
        return "food_level";
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
        return player.getFoodLevel() < threshold;
    }
}
