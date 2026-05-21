package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import gg.fotia.enchantment.core.EnchantmentData;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连杀次数条件 - 玩家连杀计数达到指定值
 * <p>由外部监听器在击杀事件中调用 {@link #recordKill(UUID)} 与
 * {@link #resetStreak(UUID)} 维护连杀计数。
 */
public class KillStreakCondition implements Condition {

    private static final Map<UUID, Integer> KILL_STREAK = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return "kill_streak";
    }

    @Override
    public boolean check(ConditionContext context) {
        Player player = context.getTriggerContext().getPlayer();
        if (player == null) {
            return false;
        }
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        if (cfg == null) {
            return false;
        }
        double threshold = context.evaluateExpression(cfg.getString("value", "0"));
        int streak = KILL_STREAK.getOrDefault(player.getUniqueId(), 0);
        return streak >= threshold;
    }

    /** 记录一次击杀 */
    public static void recordKill(UUID uid) {
        if (uid == null) return;
        KILL_STREAK.merge(uid, 1, Integer::sum);
    }

    /** 重置连杀（玩家死亡时调用） */
    public static void resetStreak(UUID uid) {
        if (uid == null) return;
        KILL_STREAK.remove(uid);
    }

    /** 获取当前连杀数 */
    public static int getStreak(UUID uid) {
        if (uid == null) return 0;
        return KILL_STREAK.getOrDefault(uid, 0);
    }
}
