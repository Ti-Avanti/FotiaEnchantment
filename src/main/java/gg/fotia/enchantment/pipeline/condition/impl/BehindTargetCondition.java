package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * 背刺条件 - 攻击者位于目标的背后
 * <p>通过比较攻击者相对目标的方向与目标朝向的夹角判断，夹角 &gt; 90° 视为背后。
 */
public class BehindTargetCondition implements Condition {

    @Override
    public String getId() {
        return "behind_target";
    }

    @Override
    public boolean check(ConditionContext context) {
        Player player = context.getTriggerContext().getPlayer();
        LivingEntity target = context.getTriggerContext().getTarget();
        if (player == null || target == null) {
            return false;
        }
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        boolean expected = cfg == null || cfg.getBoolean("value", true);

        Vector targetFacing = target.getLocation().getDirection().setY(0).normalize();
        Vector toAttacker = player.getLocation().toVector()
                .subtract(target.getLocation().toVector()).setY(0);
        if (toAttacker.lengthSquared() < 1.0E-6) {
            return !expected;
        }
        toAttacker.normalize();
        double dot = targetFacing.dot(toAttacker);
        // dot < 0 即夹角 > 90°，攻击者在目标背后
        boolean behind = dot < 0;
        return behind == expected;
    }
}
