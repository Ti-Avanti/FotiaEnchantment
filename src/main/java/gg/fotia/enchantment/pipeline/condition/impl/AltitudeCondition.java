package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import org.bukkit.entity.Player;

/**
 * 高度条件 - 玩家所在Y轴高度位于 [min, max] 范围
 */
public class AltitudeCondition implements Condition {

    @Override
    public String getId() {
        return "altitude";
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
        double min = context.evaluateExpression(cfg.getString("min", String.valueOf(Integer.MIN_VALUE)));
        double max = context.evaluateExpression(cfg.getString("max", String.valueOf(Integer.MAX_VALUE)));
        double y = player.getLocation().getY();
        return y >= min && y <= max;
    }
}
