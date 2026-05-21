package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * 粒子效果 - 在目标或玩家位置播放粒子
 *
 * <p>参数：
 * <ul>
 *     <li>particle - 粒子类型名称（如 FLAME, CLOUD），默认 CRIT</li>
 *     <li>count - 数量，默认 10</li>
 *     <li>offset - 各轴向偏移半径，默认 0.5</li>
 *     <li>at - 播放位置：SELF(自己) 或 TARGET(目标)，默认 TARGET</li>
 * </ul>
 */
public class ParticleEffect implements Effect {

    @Override
    public String getId() {
        return "PARTICLE";
    }

    @Override
    public void execute(EffectContext context) {
        String particleName = context.getExtraParam("particle", "CRIT").toUpperCase(Locale.ROOT);
        Particle particle;
        try {
            particle = Particle.valueOf(particleName);
        } catch (IllegalArgumentException ex) {
            return;
        }

        int count = context.getIntParam("count", 10);
        double offset = context.getDoubleParam("offset", 0.5);

        String at = context.getExtraParam("at", "TARGET").toUpperCase(Locale.ROOT);
        Location location = resolveLocation(at, context);
        if (location == null) return;

        World world = location.getWorld();
        if (world == null) return;

        world.spawnParticle(particle, location, Math.max(1, count), offset, offset, offset, 0);
    }

    private Location resolveLocation(String at, EffectContext context) {
        Player player = context.getTriggerContext().getPlayer();
        LivingEntity target = context.getTriggerContext().getTarget();

        if ("SELF".equals(at)) {
            return player != null ? player.getLocation().add(0, 1, 0) : null;
        }
        if (target != null) {
            return target.getLocation().add(0, 1, 0);
        }
        return player != null ? player.getLocation().add(0, 1, 0) : null;
    }
}
