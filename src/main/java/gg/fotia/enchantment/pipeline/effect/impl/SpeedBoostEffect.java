package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 速度提升效果 - 给予 SPEED 药水效果
 *
 * <p>参数：
 * <ul>
 *     <li>value - 等级（0 表示 I 级）</li>
 *     <li>duration - 持续 ticks，默认 100</li>
 * </ul>
 */
public class SpeedBoostEffect implements Effect {

    @Override
    public String getId() {
        return "SPEED_BOOST";
    }

    @Override
    public void execute(EffectContext context) {
        Player player = context.getTriggerContext().getPlayer();
        if (player == null) return;

        String valueStr = context.getConfigValue();
        int amplifier = 0;
        if (valueStr != null && !valueStr.isEmpty()) {
            amplifier = (int) Math.max(0, context.evaluateExpression(valueStr));
        }
        int duration = context.getIntParam("duration", 100);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Math.max(1, duration), amplifier, true, true, true));
    }
}
