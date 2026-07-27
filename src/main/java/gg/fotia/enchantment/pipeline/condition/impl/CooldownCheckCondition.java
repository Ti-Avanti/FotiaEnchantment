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
 *   <li>key: 冷却 key（不同条件可独立设置；实际存储时按附魔ID隔离，避免跨附魔共享冷却）</li>
 *   <li>value: 冷却时长（tick），可为表达式</li>
 * </ul>
 * 检查阶段只读; 冷却在本次动作真正执行后才记录,
 * 避免后续条件失败或动作未执行时白白消耗冷却。
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

        long ticks = cooldownTicks(context, cfg);
        if (ticks <= 0) {
            return true;
        }

        Map<String, Long> map = COOLDOWNS.get(player.getUniqueId());
        if (map == null) {
            return true;
        }
        Long expireAt = map.get(scopedKey(context, cfg));
        return expireAt == null || System.currentTimeMillis() >= expireAt;
    }

    @Override
    public boolean requiresPostCommit() {
        return true;
    }

    @Override
    public void onEffectsExecuted(ConditionContext context) {
        Player player = context.getTriggerContext().getPlayer();
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        if (player == null || cfg == null) {
            return;
        }
        long ticks = cooldownTicks(context, cfg);
        if (ticks <= 0) {
            return;
        }
        COOLDOWNS.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(scopedKey(context, cfg), System.currentTimeMillis() + ticks * MS_PER_TICK);
    }

    private long cooldownTicks(ConditionContext context, EnchantmentData.ConditionConfig cfg) {
        String valueStr = cfg.getString("value", "0");
        return (long) context.evaluateExpression(valueStr);
    }

    /**
     * 冷却键按附魔ID隔离, 默认 key 不再跨附魔共享
     */
    private String scopedKey(ConditionContext context, EnchantmentData.ConditionConfig cfg) {
        String key = cfg.getString("key", "default");
        String enchantId = context.getEnchantId();
        return enchantId == null || enchantId.isEmpty() ? key : enchantId + ":" + key;
    }

    /**
     * 清除某玩家所有冷却数据（玩家退出时调用）
     */
    public static void clearPlayer(UUID uid) {
        if (uid != null) {
            COOLDOWNS.remove(uid);
        }
    }

    public static void clearAll() {
        COOLDOWNS.clear();
    }
}
