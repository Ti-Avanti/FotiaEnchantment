package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import gg.fotia.enchantment.core.EnchantmentData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * 附近玩家数量条件 - 在指定半径内的玩家数量位于 [min, max] 之间
 */
public class PlayersNearbyCondition implements Condition {

    private static final String MAX_DEFAULT = String.valueOf(Integer.MAX_VALUE);

    @Override
    public String getId() {
        return "players_nearby";
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
        double radius = context.evaluateExpression(cfg.getString("radius", "10"));
        double min = context.evaluateExpression(cfg.getString("min", "0"));
        String maxRaw = cfg.getString("max", null);
        double max = maxRaw == null || MAX_DEFAULT.equals(maxRaw)
                ? Integer.MAX_VALUE
                : context.evaluateExpression(maxRaw);

        Collection<Entity> nearby = player.getNearbyEntities(radius, radius, radius);
        int count = 0;
        for (Entity e : nearby) {
            if (e instanceof Player && !e.equals(player)) {
                count++;
            }
        }
        return count >= min && count <= max;
    }
}
