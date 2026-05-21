package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import gg.fotia.enchantment.core.EnchantmentData;
import org.bukkit.entity.Player;

/**
 * 距上次受伤间隔条件 - 玩家距上次受伤的tick数 ≥ 指定值
 */
public class LastDamageIntervalCondition implements Condition {

    private static final long MS_PER_TICK = 50L;

    @Override
    public String getId() {
        return "last_damage_interval";
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
        double minTicks = context.evaluateExpression(cfg.getString("value", "0"));
        Long last = LastDamageTracker.getLastDamageTime(player.getUniqueId());
        if (last == null) {
            return true;
        }
        long elapsedTicks = (System.currentTimeMillis() - last) / MS_PER_TICK;
        return elapsedTicks >= minTicks;
    }
}
