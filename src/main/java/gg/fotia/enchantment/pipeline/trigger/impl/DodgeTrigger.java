package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

/**
 * 闪避触发器 - 由自定义闪避机制主动驱动
 *
 * <p>闪避是由 DodgeEffect 等自定义机制驱动的，不监听原版事件。
 * 由触发方主动调用 {@link #fire(EffectPipeline, Player, LivingEntity, Event, double)} 来触发管道。
 */
public class DodgeTrigger implements Trigger {

    private static final String ID = "DODGE";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void register(EffectPipeline pipeline) {
        // 闪避不是原版事件，由外部自定义机制调用 fire() 触发。
    }

    @Override
    public void unregister() {
        // 无内置监听器，无需注销。
    }

    /**
     * 由外部（例如 DodgeEffect）主动触发闪避事件
     *
     * @param pipeline 效果管道
     * @param player   闪避者
     * @param attacker 攻击者（可为空）
     * @param event    原始事件（可为空）
     * @param value    闪避相关的数值（如躲掉的伤害）
     */
    public static void fire(EffectPipeline pipeline,
                            Player player,
                            LivingEntity attacker,
                            Event event,
                            double value) {
        if (pipeline == null || player == null) {
            return;
        }
        ItemStack chest = player.getInventory().getChestplate();
        ItemStack item = (chest != null && !chest.getType().isAir())
                ? chest : player.getInventory().getItemInMainHand();

        TriggerContext context = TriggerContext.builder()
                .player(player)
                .target(attacker)
                .event(event)
                .item(item)
                .value(value)
                .altValue(0)
                .triggerId(ID)
                .build();
        pipeline.execute(context);
    }
}
