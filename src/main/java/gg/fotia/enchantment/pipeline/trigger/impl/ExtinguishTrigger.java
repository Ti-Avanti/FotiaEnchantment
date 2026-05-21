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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 灭火触发器
 *
 * <p>原版 Minecraft 没有灭火事件，这里通过定时检查玩家 fireTicks 是否从 >0 变为 0 来近似。
 */
public class ExtinguishTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;
    private BukkitRunnable task;
    private final Set<UUID> burning = new HashSet<>();

    @Override
    public String getId() {
        return "EXTINGUISH";
    }

    @Override
    public void register(EffectPipeline pipeline) {
        this.pipeline = pipeline;
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    boolean wasBurning = burning.contains(uuid);
                    boolean isBurning = player.getFireTicks() > 0;
                    if (wasBurning && !isBurning) {
                        TriggerContext ctx = TriggerContext.builder()
                                .player(player)
                                .item(player.getInventory().getItemInMainHand())
                                .value(1)
                                .altValue(0)
                                .triggerId(getId())
                                .build();
                        ExtinguishTrigger.this.pipeline.execute(ctx);
                    }
                    if (isBurning) {
                        burning.add(uuid);
                    } else {
                        burning.remove(uuid);
                    }
                }
            }
        };
        task.runTaskTimer(FotiaEnchantment.getInstance(), 5L, 5L);
    }

    @Override
    public void unregister() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
            task = null;
        }
        burning.clear();
        HandlerList.unregisterAll(this);
    }
}
