package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import org.bukkit.World;
import gg.fotia.enchantment.core.EnchantmentData;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 天气条件 - 判断玩家所在世界天气是否匹配
 * <p>支持 CLEAR / RAIN / THUNDER。
 */
public class WeatherCondition implements Condition {

    @Override
    public String getId() {
        return "weather";
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
        List<String> weathers = cfg.getStringList("value");
        if (weathers.isEmpty()) {
            String single = cfg.getString("value");
            if (single == null || single.isEmpty()) {
                return false;
            }
            weathers = List.of(single);
        }

        World world = player.getWorld();
        String current;
        if (world.isThundering()) {
            current = "THUNDER";
        } else if (world.hasStorm()) {
            current = "RAIN";
        } else {
            current = "CLEAR";
        }
        for (String w : weathers) {
            if (w != null && w.equalsIgnoreCase(current)) {
                return true;
            }
        }
        return false;
    }
}
