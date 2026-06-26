package gg.fotia.enchantment.pipeline.effect.impl;

import gg.fotia.enchantment.pipeline.effect.Effect;
import gg.fotia.enchantment.pipeline.effect.EffectContext;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
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
        double multiplier = multiplier(context);
        if (multiplier <= 1.0) return;

        if (event instanceof BlockDropItemEvent dropEvent) {
            multiplyDroppedItems(dropEvent.getItems(), multiplier);
            return;
        }
        if (event instanceof BlockBreakEvent breakEvent) {
            multiplyBlockBreakDrops(context, breakEvent, multiplier);
        }
    }

    private void multiplyDroppedItems(List<Item> items, double multiplier) {
        for (Item itemEntity : items) {
            ItemStack stack = itemEntity.getItemStack();
            if (stack == null) continue;
            stack.setAmount(multipliedAmount(stack, multiplier));
            itemEntity.setItemStack(stack);
        }
    }

    private void multiplyBlockBreakDrops(EffectContext context, BlockBreakEvent event, double multiplier) {
        if (!event.isDropItems()) {
            return;
        }

        ItemStack tool = context.getTriggerContext().getItem();
        Collection<ItemStack> drops = event.getBlock().getDrops(tool, event.getPlayer());
        List<ItemStack> replacementDrops = new ArrayList<>();
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType().isAir() || drop.getAmount() <= 0) {
                continue;
            }
            ItemStack replacement = drop.clone();
            replacement.setAmount(multipliedAmount(replacement, multiplier));
            replacementDrops.add(replacement);
        }
        if (replacementDrops.isEmpty()) {
            return;
        }

        event.setDropItems(false);
        Location location = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack replacement : replacementDrops) {
            event.getBlock().getWorld().dropItemNaturally(location, replacement);
        }
    }

    private double multiplier(EffectContext context) {
        String explicitMultiplier = context.getExtraParam("multiplier");
        if (explicitMultiplier != null && !explicitMultiplier.isEmpty()) {
            return context.evaluateExpression(explicitMultiplier);
        }

        String value = context.getConfigValue();
        if (value == null || value.isEmpty()) {
            return 1.0D;
        }
        double bonus = context.evaluateExpression(value);
        return bonus <= 0.0D ? 1.0D : 1.0D + bonus;
    }

    static int multipliedAmount(ItemStack stack, double multiplier) {
        int original = stack.getAmount();
        int newAmount = (int) Math.max(1, Math.ceil(original * multiplier));
        return Math.min(newAmount, stack.getMaxStackSize());
    }
}
