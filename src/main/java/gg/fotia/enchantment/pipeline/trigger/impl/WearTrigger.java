package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * 穿戴触发器 - 每秒检查玩家 4 个护甲槽，对每件非空护甲单独触发
 */
public class WearTrigger implements Trigger {

    private static final long INTERVAL_TICKS = 20L;

    private EffectPipeline pipeline;
    private BukkitTask task;

    @Override
    public String getId() {
        return "WEAR";
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
                    PlayerInventory inv = player.getInventory();
                    fireIfPresent(player, inv.getHelmet());
                    fireIfPresent(player, inv.getChestplate());
                    fireIfPresent(player, inv.getLeggings());
                    fireIfPresent(player, inv.getBoots());
                }
            }
        }.runTaskTimer(FotiaEnchantment.getInstance(), INTERVAL_TICKS, INTERVAL_TICKS);
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
        pipeline.execute(ctx);
    }

    @Override
    public void unregister() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
