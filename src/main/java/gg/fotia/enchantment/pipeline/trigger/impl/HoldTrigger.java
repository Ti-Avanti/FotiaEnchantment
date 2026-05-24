package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * 持握触发器 - 每秒检查玩家主手物品，对手中持有物品的玩家持续触发
 */
public class HoldTrigger implements Trigger {

    private static final long INTERVAL_TICKS = 20L;

    private EffectPipeline pipeline;
    private BukkitTask task;

    @Override
    public String getId() {
        return "HOLD";
    }

    @Override
    public void register(EffectPipeline pipeline) {
        this.pipeline = pipeline;
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player == null) {
                        continue;
                    }
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand == null || hand.getType().isAir()) {
                        continue;
                    }
                    if (!HoldTrigger.this.pipeline.hasActiveEnchantment(hand, getId())) {
                        continue;
                    }
                    TriggerContext ctx = TriggerContext.builder()
                            .player(player)
                            .item(hand)
                            .value(0)
                            .altValue(0)
                            .triggerId(getId())
                            .build();
                    HoldTrigger.this.pipeline.execute(ctx);
                }
            }
        }.runTaskTimer(FotiaEnchantment.getInstance(), INTERVAL_TICKS, INTERVAL_TICKS);
    }

    @Override
    public void unregister() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
