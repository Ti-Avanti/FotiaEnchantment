package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * 自定义定时触发器 - 间隔从 config.yml 的 triggers.timer-custom-interval 读取(默认 200 tick)
 */
public class TimerCustomTrigger implements Trigger {

    private static final long DEFAULT_INTERVAL_TICKS = 200L;

    private EffectPipeline pipeline;
    private BukkitTask task;

    @Override
    public String getId() {
        return "TIMER_CUSTOM";
    }

    @Override
    public void register(EffectPipeline pipeline) {
        this.pipeline = pipeline;
        long interval = DEFAULT_INTERVAL_TICKS;
        try {
            YamlConfiguration mainConfig = FotiaEnchantment.getInstance().getConfigManager().getMainConfig();
            if (mainConfig != null) {
                interval = mainConfig.getInt("triggers.timer-custom-interval", (int) DEFAULT_INTERVAL_TICKS);
            }
        } catch (Throwable ignored) {
            // 配置不可用时使用默认值
        }
        if (interval <= 0L) {
            interval = DEFAULT_INTERVAL_TICKS;
        }
        final long intervalTicks = interval;
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
                    TimerCustomTrigger.this.pipeline.execute(ctx);
                }
            }
        }.runTaskTimer(FotiaEnchantment.getInstance(), intervalTicks, intervalTicks);
    }

    @Override
    public void unregister() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
