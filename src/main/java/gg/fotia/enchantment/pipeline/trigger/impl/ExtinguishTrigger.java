package gg.fotia.enchantment.pipeline.trigger.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.pipeline.trigger.Trigger;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import gg.fotia.enchantment.util.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 灭火触发器
 *
 * <p>原版 Minecraft 没有灭火事件，这里通过定时检查玩家 fireTicks 是否从 >0 变为 0 来近似。
 */
public class ExtinguishTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;
    private Object task;
    private final Set<UUID> burning = ConcurrentHashMap.newKeySet();
    private volatile boolean active;

    @Override
    public String getId() {
        return "EXTINGUISH";
    }

    @Override
    public void register(EffectPipeline pipeline) {
        this.pipeline = pipeline;
        this.active = true;
        FotiaEnchantment plugin = FotiaEnchantment.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.task = SchedulerUtils.runTaskTimer(plugin, this::tick, 5L, 5L);
    }

    private void tick() {
        if (!active) {
            return;
        }
        for (Player player : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            dispatchPlayer(player);
        }
    }

    private void dispatchPlayer(Player player) {
        SchedulerUtils.runEntityTask(FotiaEnchantment.getInstance(), player, () -> {
            if (active && player.isOnline()) {
                handlePlayer(player);
            }
        });
    }

    private void handlePlayer(Player player) {
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
            pipeline.execute(ctx);
        }
        if (isBurning) {
            burning.add(uuid);
        } else {
            burning.remove(uuid);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        burning.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void unregister() {
        active = false;
        SchedulerUtils.cancelTask(task);
        task = null;
        burning.clear();
        HandlerList.unregisterAll(this);
    }
}
