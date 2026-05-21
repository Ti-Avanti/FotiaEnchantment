package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 冷却检查条件 - 额外冷却限制
 * <p>config 字段：
 * <ul>
 *   <li>key: 冷却 key（不同条件可独立设置）</li>
 *   <li>value: 冷却时长（tick），可为表达式</li>
 * </ul>
 * 满足时即记录冷却；冷却中则不满足。
 */
public class CooldownCheckCondition implements Condition {

    /** 静态冷却存储：玩家UUID -> (key -> 过期时间ms) */
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new ConcurrentHashMap<>();
    private static final long MS_PER_TICK = 50L;

    @Override
    public String getId() {
        return "cooldown_check";
    }

    @Override
    public boolean check(ConditionContext context) {
        Player player = context.getTriggerContext().getPlayer();
        if (player == null) {
            return false;
        }
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        if (cfg == null) {
            return true;
        }

        String key = cfg.getString("key", "default");
        String valueStr = cfg.getString("value", "0");
        long ticks = (long) context.evaluateExpression(valueStr);
        if (ticks <= 0) {
            return true;
        }

        UUID uid = player.getUniqueId();
        Map<String, Long> map = COOLDOWNS.computeIfAbsent(uid, k -> new ConcurrentHashMap<>());
        long now = System.currentTimeMillis();
        Long expireAt = map.get(key);
        if (expireAt != null && now < expireAt) {
            return false;
        }
        map.put(key, now + ticks * MS_PER_TICK);
        return true;
    }

    /**
     * 清除某玩家所有冷却数据（玩家退出时调用）
     */
    public static void clearPlayer(UUID uid) {
        if (uid != null) {
            COOLDOWNS.remove(uid);
        }
    }
}
