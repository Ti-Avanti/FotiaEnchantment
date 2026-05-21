package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import gg.fotia.enchantment.core.EnchantmentData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * 速度高于条件 - 玩家当前移动速度（方块/tick）高于指定阈值
 */
public class VelocityAboveCondition implements Condition {

    @Override
    public String getId() {
        return "velocity_above";
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
        double threshold = context.evaluateExpression(cfg.getString("value", "0"));
        Vector v = player.getVelocity();
        double speed = v.length();
        return speed > threshold;
    }
}
