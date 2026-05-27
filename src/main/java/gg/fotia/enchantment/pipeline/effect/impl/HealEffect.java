package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.compat.BukkitAttributes;
import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.entity.Player;

/**
 * 治疗效果 - 恢复玩家生命值
 */
public class HealEffect implements Effect {

    @Override
    public String getId() {
        return "HEAL";
    }

    @Override
    public void execute(EffectContext context) {
        Player player = context.getTriggerContext().getPlayer();
        if (player == null || player.isDead()) return;

        String valueStr = context.getConfigValue();
        if (valueStr == null || valueStr.isEmpty()) return;

        double amount = context.evaluateExpression(valueStr);
        if (amount <= 0) return;

        double maxHealth = BukkitAttributes.maxHealthValue(player);

        double newHealth = Math.min(maxHealth, player.getHealth() + amount);
        player.setHealth(newHealth);
    }
}
