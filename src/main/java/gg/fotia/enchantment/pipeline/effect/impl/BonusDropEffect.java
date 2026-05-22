package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 额外掉落效果 - 修改 BlockDropItemEvent 中所有掉落物的数量
 */
public class BonusDropEffect implements Effect {

    @Override
    public String getId() {
        return "BONUS_DROP";
    }

    @Override
    public void execute(EffectContext context) {
        Event event = context.getTriggerContext().getEvent();
        if (!(event instanceof BlockDropItemEvent dropEvent)) return;

        String valueStr = context.getExtraParam("multiplier", context.getConfigValue());
        if (valueStr == null || valueStr.isEmpty()) return;

        double multiplier = context.evaluateExpression(valueStr);
        if (multiplier <= 1.0) return;

        List<Item> items = dropEvent.getItems();
        for (Item itemEntity : items) {
            ItemStack stack = itemEntity.getItemStack();
            if (stack == null) continue;
            int original = stack.getAmount();
            int newAmount = (int) Math.max(1, Math.floor(original * multiplier));
            int maxStack = stack.getMaxStackSize();
            stack.setAmount(Math.min(newAmount, maxStack));
            itemEntity.setItemStack(stack);
        }
    }
}
