package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * 移除自身的药水效果
 *
 * <p>参数：potion - 药水类型 ID
 */
public class RemovePotionEffect implements Effect {

    @Override
    public String getId() {
        return "REMOVE_POTION";
    }

    @Override
    public void execute(EffectContext context) {
        Player player = context.getTriggerContext().getPlayer();
        if (player == null) return;

        String potionName = context.getExtraParam("potion");
        if (potionName == null || potionName.isEmpty()) return;

        PotionEffectType type = AddPotionSelfEffect.resolvePotionType(potionName);
        if (type == null) return;

        if (player.hasPotionEffect(type)) {
            player.removePotionEffect(type);
        }
    }
}
