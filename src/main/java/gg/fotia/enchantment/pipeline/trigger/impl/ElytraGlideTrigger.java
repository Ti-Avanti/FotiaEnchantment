package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * 鞘翅滑翔触发器 - 每 tick 检查滑翔玩家并按速度触发
 */
public class ElytraGlideTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;
    private BukkitRunnable task;

    @Override
    public String getId() {
        return "ELYTRA_GLIDE";
    }

    @Override
    public void register(EffectPipeline pipeline) {
        this.pipeline = pipeline;
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.isGliding()) {
                        continue;
                    }
                    if (!ElytraGlideTrigger.this.pipeline.hasActiveEnchantment(player, getId())) {
                        continue;
                    }
                    Vector v = player.getVelocity();
                    double speed = v == null ? 0 : v.length();
                    TriggerContext context = TriggerContext.builder()
                            .player(player)
                            .item(player.getInventory().getItemInMainHand())
                            .value(speed)
                            .altValue(0)
                            .triggerId(getId())
                            .build();
                    ElytraGlideTrigger.this.pipeline.execute(context);
                }
            }
        };
        task.runTaskTimer(FotiaEnchantment.getInstance(), 1L, 1L);
    }

    @Override
    public void unregister() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
            task = null;
        }
        HandlerList.unregisterAll(this);
    }
}
