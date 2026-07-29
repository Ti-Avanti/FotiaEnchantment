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
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 鞘翅滑翔触发器 - 事件驱动维护滑翔玩家集合, 只对滑翔中的玩家按间隔触发。
 * 检查间隔读取 config.yml 的 performance.effect-check-interval。
 */
public class ElytraGlideTrigger implements Trigger, Listener {

    private EffectPipeline pipeline;
    private Object task;
    private final Set<UUID> glidingPlayers = ConcurrentHashMap.newKeySet();
    private volatile boolean active;

    @Override
    public String getId() {
        return "ELYTRA_GLIDE";
    }

    @Override
    public void register(EffectPipeline pipeline) {
        this.pipeline = pipeline;
        this.active = true;
        FotiaEnchantment plugin = FotiaEnchantment.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // reload 时补录已在滑翔中的玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            registerGlidingPlayer(player);
        }
        long interval = Math.max(1L, effectCheckInterval());
        this.task = SchedulerUtils.runTaskTimer(plugin, this::tick, interval, interval);
    }

    private long effectCheckInterval() {
        try {
            return FotiaEnchantment.getInstance().getConfigManager().getEffectCheckInterval();
        } catch (Throwable ignored) {
            return 1L;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.isGliding()) {
            glidingPlayers.add(player.getUniqueId());
        } else {
            glidingPlayers.remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        glidingPlayers.remove(event.getPlayer().getUniqueId());
    }

    private void tick() {
        if (!active || glidingPlayers.isEmpty()) {
            return;
        }
        for (UUID playerId : new ArrayList<>(glidingPlayers)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                glidingPlayers.remove(playerId);
                continue;
            }
            dispatchGlidingPlayer(player, playerId);
        }
    }

    private void registerGlidingPlayer(Player player) {
        SchedulerUtils.runEntityTask(FotiaEnchantment.getInstance(), player, () -> {
            if (active && player.isOnline() && player.isGliding()) {
                glidingPlayers.add(player.getUniqueId());
            }
        });
    }

    private void dispatchGlidingPlayer(Player player, UUID playerId) {
        SchedulerUtils.runEntityTask(FotiaEnchantment.getInstance(), player, () -> {
            if (active) {
                handleGlidingPlayer(player, playerId);
            }
        });
    }

    private void handleGlidingPlayer(Player player, UUID playerId) {
        if (!player.isOnline() || !player.isGliding()) {
            glidingPlayers.remove(playerId);
            return;
        }
        Vector velocity = player.getVelocity();
        double speed = velocity == null ? 0D : velocity.length();
        TriggerContext context = TriggerContext.builder()
                .player(player)
                .item(player.getInventory().getItemInMainHand())
                .value(speed)
                .altValue(0)
                .triggerId(getId())
                .build();
        pipeline.execute(context);
    }

    @Override
    public void unregister() {
        active = false;
        SchedulerUtils.cancelTask(task);
        task = null;
        glidingPlayers.clear();
        HandlerList.unregisterAll(this);
    }
}
