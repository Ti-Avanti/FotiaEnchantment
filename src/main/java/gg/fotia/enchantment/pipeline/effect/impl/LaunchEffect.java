package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Locale;

/**
 * 击飞效果 - 设置目标的速度向量
 *
 * <p>参数：
 * <ul>
 *     <li>value - 力度</li>
 *     <li>direction - 方向：UP(向上)、AWAY(远离攻击者)、TOWARD(朝向攻击者)，默认 UP</li>
 * </ul>
 */
public class LaunchEffect implements Effect {

    @Override
    public String getId() {
        return "LAUNCH";
    }

    @Override
    public void execute(EffectContext context) {
        LivingEntity target = context.getTriggerContext().getTarget();
        if (target == null || target.isDead()) return;

        String valueStr = context.getConfigValue();
        if (valueStr == null || valueStr.isEmpty()) return;

        double power = context.evaluateExpression(valueStr);
        if (power <= 0) return;

        String direction = context.getExtraParam("direction", "UP").toUpperCase(Locale.ROOT);
        Player attacker = context.getTriggerContext().getPlayer();

        Vector velocity;
        switch (direction) {
            case "AWAY" -> {
                if (attacker == null) {
                    velocity = new Vector(0, power, 0);
                } else {
                    Vector away = target.getLocation().toVector()
                            .subtract(attacker.getLocation().toVector());
                    if (away.lengthSquared() < 1.0E-6) {
                        away = new Vector(0, 1, 0);
                    }
                    velocity = away.normalize().multiply(power).setY(power * 0.5);
                }
            }
            case "TOWARD" -> {
                if (attacker == null) {
                    velocity = new Vector(0, power, 0);
                } else {
                    Vector toward = attacker.getLocation().toVector()
                            .subtract(target.getLocation().toVector());
                    if (toward.lengthSquared() < 1.0E-6) {
                        toward = new Vector(0, 1, 0);
                    }
                    velocity = toward.normalize().multiply(power).setY(power * 0.5);
                }
            }
            default -> velocity = new Vector(0, power, 0);
        }

        target.setVelocity(velocity);
    }
}
