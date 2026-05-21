package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 * 购买物品触发器 - 外部经济插件驱动
 *
 * <p>需要外部经济插件 hook 来主动调用 {@link #fire(Player, ItemStack, double)}。
 * 当前 Bukkit/Paper 原生事件无对应概念，因此 register 方法为空。
 */
public class BuyItemTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;

    @Override
    public String getId() {
        return "BUY_ITEM";
    }

    @Override
    public void register(EffectPipeline pipeline) {
        this.pipeline = pipeline;
        // Bukkit/Paper 没有通用购买事件，保留给经济插件 hook 主动触发。
    }

    @Override
    public void unregister() {
        // 无内置监听器，无需取消注册。
    }

    /**
     * 由外部经济插件调用，主动触发本触发器
     *
     * @param player 购买物品的玩家
     * @param item   购买的物品
     * @param price  购买价格（写入 value）
     */
    public void fire(Player player, ItemStack item, double price) {
        if (pipeline == null || player == null) {
            return;
        }
        TriggerContext context = TriggerContext.builder()
                .player(player)
                .item(item)
                .value(price)
                .altValue(0)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }
}
