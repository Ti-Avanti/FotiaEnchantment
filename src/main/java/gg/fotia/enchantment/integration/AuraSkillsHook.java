package gg.fotia.enchantment.integration;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AuraSkills 集成钩子
 * 提供获取玩家技能等级的能力
 */
public class AuraSkillsHook {

    private final Logger logger;
    private boolean available;

    public AuraSkillsHook(Logger logger) {
        this.logger = logger;
        this.available = Bukkit.getPluginManager().getPlugin("AuraSkills") != null;
        if (available) {
            logger.info("已检测到 AuraSkills，集成已启用");
        }
    }

    /**
     * 检查 AuraSkills 是否可用
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * 获取玩家指定技能的等级
     *
     * @param player 玩家
     * @param skill  技能名称（如 farming, mining 等）
     * @return 技能等级，不可用时返回 0
     */
    public int getSkillLevel(Player player, String skill) {
        if (!available || player == null || skill == null || skill.isEmpty()) {
            return 0;
        }
        try {
            Skills auraSkill = parseSkill(skill);
            if (auraSkill == null) {
                return 0;
            }
            AuraSkillsApi api = AuraSkillsApi.get();
            SkillsUser user = api.getUser(player.getUniqueId());
            if (user == null || !user.isLoaded()) {
                return 0;
            }
            return user.getSkillLevel(auraSkill);
        } catch (IllegalArgumentException e) {
            return 0;
        } catch (IllegalStateException e) {
            available = false;
            return 0;
        } catch (Exception e) {
            logger.log(Level.WARNING, "调用 AuraSkills API 时出错", e);
            return 0;
        }
    }

    private Skills parseSkill(String skill) {
        String normalized = skill.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash < normalized.length() - 1) {
            normalized = normalized.substring(slash + 1);
        }
        try {
            return Skills.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
