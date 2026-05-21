package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

/**
 * 闪避效果 - 取消当前伤害事件
 */
public class DodgeEffect implements Effect {

    @Override
    public String getId() {
        return "DODGE";
    }

    @Override
    public void execute(EffectContext context) {
        Event event = context.getTriggerContext().getEvent();
        if (event instanceof Cancellable cancellable) {
            cancellable.setCancelled(true);
        }
    }
}
