package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 持握触发器 - 每秒检查玩家主手物品，对手中持有物品的玩家持续触发。
 * 使用槽位限定执行, 只处理主手物品的附魔, 不会连带触发穿着护甲上的效果。
 */
public class HoldTrigger extends AbstractPlayerTimerTrigger {

    @Override
    public String getId() {
        return "HOLD";
    }

    @Override
    protected long intervalTicks() {
        return 20L;
    }

    @Override
    protected void handlePlayer(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            return;
        }
        TriggerContext ctx = TriggerContext.builder()
                .player(player)
                .item(hand)
                .value(0)
                .altValue(0)
                .triggerId(getId())
                .build();
        pipeline().executeForItem(ctx, hand);
    }
}
