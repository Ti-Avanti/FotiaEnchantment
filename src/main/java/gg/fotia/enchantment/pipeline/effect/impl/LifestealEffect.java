package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

/**
 * 生命偷取效果 - 按造成伤害的百分比回复自身生命
 *
 * <p>value 取值范围 0-100+，表示偷取的百分比。
 * 例如 value=20 表示造成 10 点伤害可回复 2 点生命。
 */
public class LifestealEffect implements Effect {

    @Override
    public String getId() {
        return "LIFESTEAL";
    }

    @Override
    public void execute(EffectContext context) {
        Player player = context.getTriggerContext().getPlayer();
        if (player == null || player.isDead()) return;

        String valueStr = context.getConfigValue();
        if (valueStr == null || valueStr.isEmpty()) return;

        double percent = context.evaluateExpression(valueStr);
        if (percent <= 0) return;

        double dealtDamage = context.getTriggerContext().getValue();
        if (dealtDamage <= 0) return;

        double healAmount = dealtDamage * (percent / 100.0);
        if (healAmount <= 0) return;

        double maxHealth = 20.0;
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            maxHealth = attr.getValue();
        }

        double newHealth = Math.min(maxHealth, player.getHealth() + healAmount);
        player.setHealth(newHealth);
    }
}
