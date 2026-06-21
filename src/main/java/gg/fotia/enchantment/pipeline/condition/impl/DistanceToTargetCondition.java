package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.LocationDistance;
import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 与目标距离条件 - 玩家与目标距离位于 [min, max] 之间
 */
public class DistanceToTargetCondition implements Condition {

    @Override
    public String getId() {
        return "distance_to_target";
    }

    @Override
    public boolean check(ConditionContext context) {
        Player player = context.getTriggerContext().getPlayer();
        LivingEntity target = context.getTriggerContext().getTarget();
        if (player == null || target == null) {
            return false;
        }
        if (!player.getWorld().equals(target.getWorld())) {
            return false;
        }
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        if (cfg == null) {
            return false;
        }
        double min = context.evaluateExpression(cfg.getString("min", "0"));
        double max = context.evaluateExpression(cfg.getString("max", String.valueOf(Integer.MAX_VALUE)));
        double dist = LocationDistance.safeDistance(player.getLocation(), target.getLocation());
        return dist >= min && dist <= max;
    }
}
