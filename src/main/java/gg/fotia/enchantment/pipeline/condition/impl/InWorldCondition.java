package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import gg.fotia.enchantment.core.EnchantmentData;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 世界条件 - 玩家所在世界名是否在列表中
 */
public class InWorldCondition implements Condition {

    @Override
    public String getId() {
        return "in_world";
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
        List<String> worlds = cfg.getStringList("value");
        if (worlds.isEmpty()) {
            String single = cfg.getString("value");
            if (single == null || single.isEmpty()) {
                return false;
            }
            worlds = List.of(single);
        }

        String name = player.getWorld().getName();
        for (String w : worlds) {
            if (w != null && w.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
