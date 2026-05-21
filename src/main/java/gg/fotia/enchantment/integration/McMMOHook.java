package gg.fotia.enchantment.integration;

import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * mcMMO 集成钩子
 * 提供获取玩家 mcMMO 技能等级的能力
 */
public class McMMOHook {

    private final Logger logger;
    private boolean available;

    public McMMOHook(Logger logger) {
        this.logger = logger;
        this.available = Bukkit.getPluginManager().getPlugin("mcMMO") != null;
        if (available) {
            logger.info("已检测到 mcMMO，集成已启用");
        }
    }

    /**
     * 检查 mcMMO 是否可用
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * 获取玩家指定技能的等级
     *
     * @param player 玩家
     * @param skill  技能名称（如 MINING, SWORDS, AXES 等）
     * @return 技能等级，不可用时返回 0
     */
    public int getSkillLevel(Player player, String skill) {
        if (!available || player == null || skill == null || skill.isEmpty()) {
            return 0;
        }
        try {
            PrimarySkillType skillType = PrimarySkillType.valueOf(skill.toUpperCase(Locale.ROOT));
            return ExperienceAPI.getLevel(player, skillType);
        } catch (IllegalArgumentException e) {
            return 0;
        } catch (Exception e) {
            logger.log(Level.WARNING, "获取 mcMMO 技能等级时出错: " + skill, e);
            return 0;
        }
    }
}
