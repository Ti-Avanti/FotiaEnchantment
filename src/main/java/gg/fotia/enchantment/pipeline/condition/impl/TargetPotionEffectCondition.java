package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import gg.fotia.enchantment.core.EnchantmentData;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * 目标药水效果条件 - 目标拥有指定药水效果之一
 */
public class TargetPotionEffectCondition implements Condition {

    @Override
    public String getId() {
        return "target_potion_effect";
    }

    @Override
    public boolean check(ConditionContext context) {
        LivingEntity target = context.getTriggerContext().getTarget();
        if (target == null) {
            return false;
        }
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        if (cfg == null) {
            return false;
        }
        List<String> effects = cfg.getStringList("value");
        if (effects.isEmpty()) {
            String single = cfg.getString("value");
            if (single == null || single.isEmpty()) {
                return false;
            }
            effects = List.of(single);
        }

        for (String effName : effects) {
            if (effName == null) continue;
            NamespacedKey key = NamespacedKey.minecraft(effName.toLowerCase());
            PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(key);
            if (type == null) continue;
            PotionEffect active = target.getPotionEffect(type);
            if (active != null) {
                return true;
            }
        }
        return false;
    }
}
