package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 给目标施加药水效果
 *
 * <p>参数：
 * <ul>
 *     <li>potion - 药水类型 ID</li>
 *     <li>duration - 持续 ticks</li>
 *     <li>amplifier - 等级</li>
 * </ul>
 */
public class AddPotionTargetEffect implements Effect {

    @Override
    public String getId() {
        return "ADD_POTION_TARGET";
    }

    @Override
    public void execute(EffectContext context) {
        LivingEntity target = context.getTriggerContext().getTarget();
        if (target == null || target.isDead()) return;

        String potionName = context.getExtraParam("potion");
        if (potionName == null || potionName.isEmpty()) return;

        PotionEffectType type = AddPotionSelfEffect.resolvePotionType(potionName);
        if (type == null) return;

        int duration = context.getIntParam("duration", 100);
        int amplifier = context.getIntParam("amplifier", 0);

        target.addPotionEffect(new PotionEffect(type, Math.max(1, duration), Math.max(0, amplifier), true, true, true));
    }
}
