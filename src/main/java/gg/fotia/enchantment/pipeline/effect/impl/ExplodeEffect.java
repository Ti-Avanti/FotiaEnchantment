package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 爆炸效果 - 在目标位置创建爆炸
 *
 * <p>参数：
 * <ul>
 *     <li>power - 威力，默认 2.0</li>
 *     <li>fire - 是否着火，默认 false</li>
 *     <li>break-blocks - 是否破坏方块，默认 false</li>
 * </ul>
 */
public class ExplodeEffect implements Effect {

    @Override
    public String getId() {
        return "EXPLODE";
    }

    @Override
    public void execute(EffectContext context) {
        float power = (float) context.getDoubleParam("power", 2.0);
        if (power <= 0) return;

        boolean fire = context.getBooleanParam("fire", false);
        boolean breakBlocks = context.getBooleanParam("break-blocks", false);

        LivingEntity target = context.getTriggerContext().getTarget();
        Player player = context.getTriggerContext().getPlayer();
        Location location = target != null ? target.getLocation() : (player != null ? player.getLocation() : null);
        if (location == null) return;

        World world = location.getWorld();
        if (world == null) return;

        if (player != null) {
            world.createExplosion(location, power, fire, breakBlocks, player);
        } else {
            world.createExplosion(location, power, fire, breakBlocks);
        }
    }
}
