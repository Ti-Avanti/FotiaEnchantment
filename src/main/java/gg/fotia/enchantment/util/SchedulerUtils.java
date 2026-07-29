package gg.fotia.enchantment.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/**
 * Paper/Folia 兼容调度器封装。
 * Paper 会将区域调度器映射到主线程，因此无需识别具体服务端实现。
 */
public class SchedulerUtils {

    /**
     * 在全局区域运行任务。
     *
     * @param plugin 插件实例
     * @param task   任务
     * @return 任务句柄
     */
    public static Object runTask(Plugin plugin, Runnable task) {
        return Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
    }

    /**
     * 在全局区域延迟运行任务。
     *
     * @param plugin     插件实例
     * @param task       任务
     * @param delayTicks 延迟tick数
     * @return 任务句柄
     */
    public static Object runTaskLater(Plugin plugin, Runnable task, long delayTicks) {
        long delay = Math.max(1, delayTicks);
        return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), delay);
    }

    /**
     * 在全局区域定时重复运行任务。
     *
     * @param plugin      插件实例
     * @param task        任务
     * @param delayTicks  初始延迟tick数
     * @param periodTicks 重复间隔tick数
     * @return 任务句柄
     */
    public static Object runTaskTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        long delay = Math.max(1, delayTicks);
        long period = Math.max(1, periodTicks);
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), delay, period);
    }

    /**
     * 在实体所在区域运行任务。
     *
     * @param plugin 插件实例
     * @param entity 目标实体
     * @param task   任务
     * @return 任务句柄
     */
    public static Object runEntityTask(Plugin plugin, Entity entity, Runnable task) {
        return runEntityTask(plugin, entity, task, null);
    }

    /**
     * 在实体所在区域运行任务，支持实体已被移除时的 retired 回调。
     *
     * @param plugin  插件实例
     * @param entity  目标实体
     * @param task    任务
     * @param retired 实体已移除时的回调, 可为 null
     * @return 任务句柄
     */
    public static Object runEntityTask(Plugin plugin, Entity entity, Runnable task, Runnable retired) {
        return entity.getScheduler().run(plugin, scheduledTask -> task.run(), retired);
    }

    /**
     * 在实体所在区域延迟运行任务。
     *
     * @param plugin     插件实例
     * @param entity     目标实体
     * @param task       任务
     * @param delayTicks 延迟tick数
     * @return 任务句柄
     */
    public static Object runEntityTaskLater(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
        long delay = Math.max(1, delayTicks);
        return entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, delay);
    }

    /**
     * 在指定位置的区域运行任务。
     *
     * @param plugin   插件实例
     * @param location 目标位置
     * @param task     任务
     * @return 任务句柄
     */
    public static Object runAtLocation(Plugin plugin, Location location, Runnable task) {
        return Bukkit.getRegionScheduler().run(plugin, location, scheduledTask -> task.run());
    }

    /**
     * 取消区域调度器任务。
     *
     * @param taskHandle 任务句柄（runTask 等方法的返回值）
     */
    public static void cancelTask(Object taskHandle) {
        if (taskHandle instanceof ScheduledTask scheduledTask) {
            scheduledTask.cancel();
        }
    }
}
