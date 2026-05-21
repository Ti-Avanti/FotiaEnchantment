package gg.fotia.enchantment.integration;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MythicMobs 集成钩子
 * 判断实体是否为 MythicMobs 生物并获取其类型
 */
public class MythicMobsHook {

    private final Logger logger;
    private boolean available;

    public MythicMobsHook(Logger logger) {
        this.logger = logger;
        this.available = Bukkit.getPluginManager().getPlugin("MythicMobs") != null;
        if (available) {
            logger.info("已检测到 MythicMobs，集成已启用");
        }
    }

    /**
     * 检查 MythicMobs 是否可用
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * 判断实体是否为 MythicMobs 实体
     *
     * @param entity 要检查的实体
     * @return 是否为 MythicMobs 实体
     */
    public boolean isMythicMob(Entity entity) {
        if (!available || entity == null) {
            return false;
        }
        try {
            return MythicBukkit.inst().getMobManager().isMythicMob(entity);
        } catch (Exception e) {
            logger.log(Level.WARNING, "检查 MythicMobs 实体时出错", e);
            return false;
        }
    }

    /**
     * 获取 MythicMobs 实体的类型名
     *
     * @param entity 要检查的实体
     * @return MM 实体类型名，不是 MM 实体或出错时返回 null
     */
    public String getMobType(Entity entity) {
        if (!available || entity == null) {
            return null;
        }
        try {
            ActiveMob activeMob = MythicBukkit.inst().getMobManager()
                    .getMythicMobInstance(entity);
            if (activeMob != null) {
                return activeMob.getMobType();
            }
            return null;
        } catch (Exception e) {
            logger.log(Level.WARNING, "获取 MythicMobs 实体类型时出错", e);
            return null;
        }
    }
}
