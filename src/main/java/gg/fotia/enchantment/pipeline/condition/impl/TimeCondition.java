package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import org.bukkit.World;
import gg.fotia.enchantment.core.EnchantmentData;
import org.bukkit.entity.Player;

/**
 * 时间条件 - 判断当前世界时间属于白天或夜晚
 */
public class TimeCondition implements Condition {

    @Override
    public String getId() {
        return "time";
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
        String value = cfg.getString("value");
        if (value == null || value.isEmpty()) {
            return false;
        }

        World world = player.getWorld();
        long time = world.getTime();
        // 白天 0-12300，夜晚 12300-23999
        boolean day = time < 12300 || time >= 23850;
        return value.equalsIgnoreCase("day") ? day : !day;
    }
}
