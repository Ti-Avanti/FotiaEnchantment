package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * 头顶露天条件 - 玩家头顶上方是否无遮挡（露天）
 */
public class ExposureToSkyCondition implements Condition {

    @Override
    public String getId() {
        return "exposure_to_sky";
    }

    @Override
    public boolean check(ConditionContext context) {
        Player player = context.getTriggerContext().getPlayer();
        if (player == null) {
            return false;
        }
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        boolean expected = cfg == null || cfg.getBoolean("value", true);

        Location loc = player.getEyeLocation();
        World world = loc.getWorld();
        if (world == null) {
            return false;
        }
        int highest = world.getHighestBlockYAt(loc);
        boolean exposed = loc.getBlockY() >= highest;
        return exposed == expected;
    }
}
