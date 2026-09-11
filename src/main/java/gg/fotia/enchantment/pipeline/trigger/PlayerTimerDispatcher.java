package gg.fotia.enchantment.pipeline.trigger;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.util.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** 合并同一 tick 到期的定时触发器，每名玩家只投递一次实体任务。 */
public final class PlayerTimerDispatcher {
    private final FotiaEnchantment plugin;
    private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();
    private Object task;
    private long tick;
    private int firstPlayer;

    public PlayerTimerDispatcher(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    public synchronized Runnable subscribe(long interval, Consumer<Player> action) {
        Subscription subscription = new Subscription(Math.max(1L, interval), action);
        subscription.nextTick = tick + subscription.interval;
        subscriptions.add(subscription);
        if (task == null) {
            task = SchedulerUtils.runTaskTimer(plugin, this::tick, 1L, 1L);
        }
        return () -> unsubscribe(subscription);
    }

    private synchronized void unsubscribe(Subscription subscription) {
        subscription.active = false;
        subscriptions.remove(subscription);
        if (subscriptions.isEmpty()) {
            SchedulerUtils.cancelTask(task);
            task = null;
        }
    }

    private void tick() {
        List<Subscription> due = new ArrayList<>();
        synchronized (this) {
            tick++;
            for (Subscription subscription : subscriptions) {
                if (subscription.active && tick >= subscription.nextTick) {
                    subscription.nextTick = tick + subscription.interval;
                    due.add(subscription);
                }
            }
        }
        if (due.isEmpty()) {
            return;
        }
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) {
            return;
        }
        // 轮换先处理的玩家，避免全服额度总由相同顺序的玩家先占用。
        int start = Math.floorMod(firstPlayer++, players.size());
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get((start + i) % players.size());
            SchedulerUtils.runEntityTask(plugin, player, () -> {
                if (!player.isOnline()) {
                    return;
                }
                for (Subscription subscription : due) {
                    if (subscription.active) {
                        try {
                            subscription.action.accept(player);
                        } catch (RuntimeException ex) {
                            plugin.getLogger().log(java.util.logging.Level.WARNING,
                                    "执行定时附魔触发器失败", ex);
                        }
                    }
                }
            });
        }
    }

    public synchronized void shutdown() {
        subscriptions.forEach(subscription -> subscription.active = false);
        subscriptions.clear();
        SchedulerUtils.cancelTask(task);
        task = null;
    }

    private static final class Subscription {
        private final long interval;
        private final Consumer<Player> action;
        private volatile boolean active = true;
        private long nextTick;

        private Subscription(long interval, Consumer<Player> action) {
            this.interval = interval;
            this.action = action;
        }
    }
}
