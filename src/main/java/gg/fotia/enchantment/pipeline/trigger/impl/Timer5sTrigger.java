package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * 定时触发器(5秒) - 每 100 tick 遍历在线玩家触发一次
 */
public class Timer5sTrigger implements Trigger {

    private static final long INTERVAL_TICKS = 100L;

    private EffectPipeline pipeline;
    private BukkitTask task;

    @Override
    public String getId() {
        return "TIMER_5S";
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
                    TriggerContext ctx = TriggerContext.builder()
                            .player(player)
                            .item(player.getInventory().getItemInMainHand())
                            .value(0)
                            .altValue(0)
                            .triggerId(getId())
                            .build();
                    Timer5sTrigger.this.pipeline.execute(ctx);
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
