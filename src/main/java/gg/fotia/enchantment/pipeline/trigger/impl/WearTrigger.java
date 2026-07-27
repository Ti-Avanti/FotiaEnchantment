package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * 穿戴触发器 - 每秒检查玩家 4 个护甲槽，对每件非空护甲单独触发。
 * 使用槽位限定执行, 每件护甲只处理自己的附魔, 避免 N 件护甲触发 N² 次效果。
 */
public class WearTrigger extends AbstractPlayerTimerTrigger {

    @Override
    public String getId() {
        return "WEAR";
    }

    @Override
    protected long intervalTicks() {
        return 20L;
    }

    @Override
    protected void handlePlayer(Player player) {
        PlayerInventory inv = player.getInventory();
        fireIfPresent(player, inv.getHelmet());
        fireIfPresent(player, inv.getChestplate());
        fireIfPresent(player, inv.getLeggings());
        fireIfPresent(player, inv.getBoots());
    }

    private void fireIfPresent(Player player, ItemStack armor) {
        if (armor == null || armor.getType().isAir()) {
            return;
        }
        TriggerContext ctx = TriggerContext.builder()
                .player(player)
                .item(armor)
                .value(0)
                .altValue(0)
                .triggerId(getId())
                .build();
        pipeline().executeForItem(ctx, armor);
    }
}
