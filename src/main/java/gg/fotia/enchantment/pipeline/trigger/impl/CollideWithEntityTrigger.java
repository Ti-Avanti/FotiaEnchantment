package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 与实体碰撞触发器
 *
 * <p>原版没有玩家碰撞实体事件，这里使用定时近距离检查作为近似实现。
 */
public class CollideWithEntityTrigger implements Trigger, Listener {

    private static final double COLLIDE_DISTANCE = 1.5;

    private EffectPipeline pipeline;
    private BukkitRunnable task;
    private final Map<UUID, Long> lastTrigger = new HashMap<>();

    @Override
    public String getId() {
        return "COLLIDE_WITH_ENTITY";
    }

    @Override
    public void register(EffectPipeline pipeline) {
        this.pipeline = pipeline;
        FotiaEnchantment plugin = FotiaEnchantment.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Long last = lastTrigger.get(player.getUniqueId());
                    if (last != null && now - last < 500L) {
                        continue;
                    }
                    if (!CollideWithEntityTrigger.this.pipeline.hasActiveEnchantment(player, getId())) {
                        continue;
                    }
                    for (Entity entity : player.getNearbyEntities(
                            COLLIDE_DISTANCE, COLLIDE_DISTANCE, COLLIDE_DISTANCE)) {
                        if (!(entity instanceof LivingEntity living)) {
                            continue;
                        }
                        if (living.equals(player)) {
                            continue;
                        }
                        TriggerContext ctx = TriggerContext.builder()
                                .player(player)
                                .target(living)
                                .item(player.getInventory().getItemInMainHand())
                                .value(1)
                                .altValue(0)
                                .triggerId(getId())
                                .build();
                        CollideWithEntityTrigger.this.pipeline.execute(ctx);
                        lastTrigger.put(player.getUniqueId(), now);
                        break;
                    }
                }
            }
        };
        task.runTaskTimer(FotiaEnchantment.getInstance(), 5L, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastTrigger.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void unregister() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
            task = null;
        }
        lastTrigger.clear();
        HandlerList.unregisterAll(this);
    }
}
