package gg.fotia.enchantment.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;

/**
 * Folia 兼容调度器封装
 * 运行时自动检测是否为 Folia 环境，根据环境选择合适的调度方式。
 */
public class SchedulerUtils {

    private static final boolean IS_FOLIA = isFoliaServer();

    /**
     * 判断当前是否为 Folia 环境
     *
     * @return 是否为 Folia
     */
    public static boolean isFolia() {
        return IS_FOLIA;
    }

    private static boolean isFoliaServer() {
        String serverName = Bukkit.getServer().getName();
        String version = Bukkit.getServer().getVersion();
        return "Folia".equalsIgnoreCase(serverName)
                || version.toLowerCase(Locale.ROOT).contains("folia");
    }

    /**
     * 在主线程/全局区域运行任务
     * Folia: 使用 GlobalRegionScheduler
     * Paper/Spigot: 使用 BukkitScheduler
     *
     * @param plugin 插件实例
     * @param task   任务
     * @return 任务句柄
     */
    public static Object runTask(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            return Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
        } else {
            return Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * 延迟运行任务
     * Folia: 使用 GlobalRegionScheduler
     * Paper/Spigot: 使用 BukkitScheduler
     *
     * @param plugin     插件实例
     * @param task       任务
     * @param delayTicks 延迟tick数
     * @return 任务句柄
     */
    public static Object runTaskLater(Plugin plugin, Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            // Folia 的延迟最小为1
            long delay = Math.max(1, delayTicks);
            return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), delay);
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * 定时重复运行任务
     * Folia: 使用 GlobalRegionScheduler
     * Paper/Spigot: 使用 BukkitScheduler
     *
     * @param plugin      插件实例
     * @param task        任务
     * @param delayTicks  初始延迟tick数
     * @param periodTicks 重复间隔tick数
     * @return 任务句柄
     */
    public static Object runTaskTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (IS_FOLIA) {
            long delay = Math.max(1, delayTicks);
            long period = Math.max(1, periodTicks);
            return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), delay, period);
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    /**
     * 在实体所在区域运行任务
     * Folia: 使用 Entity 的调度器
     * Paper/Spigot: 使用 BukkitScheduler
     *
     * @param plugin 插件实例
     * @param entity 目标实体
     * @param task   任务
     * @return 任务句柄
     */
    public static Object runEntityTask(Plugin plugin, Entity entity, Runnable task) {
        if (IS_FOLIA) {
            return entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
        } else {
            return Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * 在实体所在区域延迟运行任务
     * Folia: 使用 Entity 的调度器
     * Paper/Spigot: 使用 BukkitScheduler
     *
     * @param plugin     插件实例
     * @param entity     目标实体
     * @param task       任务
     * @param delayTicks 延迟tick数
     * @return 任务句柄
     */
    public static Object runEntityTaskLater(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            long delay = Math.max(1, delayTicks);
            return entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, delay);
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * 在指定位置的区域运行任务
     * Folia: 使用 RegionScheduler
     * Paper/Spigot: 使用 BukkitScheduler
     *
     * @param plugin   插件实例
     * @param location 目标位置
     * @param task     任务
     * @return 任务句柄
     */
    public static Object runAtLocation(Plugin plugin, Location location, Runnable task) {
        if (IS_FOLIA) {
            return Bukkit.getRegionScheduler().run(plugin, location, scheduledTask -> task.run());
        } else {
            return Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * 取消任务
     * 支持 Folia 的 ScheduledTask 和 Bukkit 的 BukkitTask
     *
     * @param taskHandle 任务句柄（runTask 等方法的返回值）
     */
    public static void cancelTask(Object taskHandle) {
        if (taskHandle == null) return;

        if (IS_FOLIA) {
            if (taskHandle instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduledTask) {
                scheduledTask.cancel();
            }
        } else {
            if (taskHandle instanceof BukkitTask bukkitTask) {
                bukkitTask.cancel();
            }
        }
    }
}
