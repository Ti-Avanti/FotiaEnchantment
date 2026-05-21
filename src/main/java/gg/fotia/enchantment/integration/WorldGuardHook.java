package gg.fotia.enchantment.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WorldGuard 集成钩子
 * 注册自定义 Flag 并提供区域内附魔许可检查
 */
public class WorldGuardHook {

    private final Logger logger;
    private boolean available;

    /**
     * 自定义 Flag: fotia-enchant-deny
     * 设置为 DENY 时阻止附魔效果在该区域触发
     */
    private static StateFlag ENCHANT_DENY_FLAG;

    public WorldGuardHook(Logger logger) {
        this.logger = logger;
        this.available = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    /**
     * 在 onLoad 阶段注册自定义 Flag
     * 必须在 WorldGuard 加载时调用（即插件 onLoad 中）
     */
    public void registerFlags() {
        if (!available) {
            return;
        }
        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            StateFlag flag = new StateFlag("fotia-enchant-deny", false);
            registry.register(flag);
            ENCHANT_DENY_FLAG = flag;
            logger.info("已注册 WorldGuard Flag: fotia-enchant-deny");
        } catch (FlagConflictException e) {
            // Flag 已被注册（可能是其他插件或重载）
            logger.warning("WorldGuard Flag 'fotia-enchant-deny' 已存在，使用已有 Flag");
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            Object existing = registry.get("fotia-enchant-deny");
            if (existing instanceof StateFlag) {
                ENCHANT_DENY_FLAG = (StateFlag) existing;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "注册 WorldGuard Flag 时出错", e);
            available = false;
        }
    }

    /**
     * 检查 WorldGuard 是否可用
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * 检查指定位置是否允许附魔效果
     *
     * @param player   玩家
     * @param location 位置
     * @return 是否允许附魔效果（Flag 为 DENY 时返回 false）
     */
    public boolean isEnchantAllowed(Player player, Location location) {
        if (!available || ENCHANT_DENY_FLAG == null) {
            // WorldGuard 不可用或 Flag 未注册，默认允许
            return true;
        }
        if (player == null || location == null) {
            return true;
        }
        try {
            RegionQuery query = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(
                    BukkitAdapter.adapt(location));
            // 设置为 DENY 时阻止附魔效果。
            StateFlag.State state = regions.queryState(
                    WorldGuardPlugin.inst().wrapPlayer(player),
                    ENCHANT_DENY_FLAG);
            return state != StateFlag.State.DENY;
        } catch (Exception e) {
            logger.log(Level.WARNING, "检查 WorldGuard 区域 Flag 时出错", e);
            return true;
        }
    }

    /**
     * 获取指定位置所在的 WorldGuard 区域 ID 集合。
     */
    public Set<String> getRegionIds(Location location) {
        if (!available || location == null) {
            return Collections.emptySet();
        }
        try {
            RegionQuery query = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .createQuery();
            ApplicableRegionSet regions = query.getApplicableRegions(
                    BukkitAdapter.adapt(location));
            Set<String> ids = new HashSet<>();
            for (ProtectedRegion region : regions.getRegions()) {
                if (region != null && region.getId() != null) {
                    ids.add(region.getId());
                }
            }
            return ids;
        } catch (Exception e) {
            logger.log(Level.WARNING, "获取 WorldGuard 区域列表时出错", e);
            return Collections.emptySet();
        }
    }
}
