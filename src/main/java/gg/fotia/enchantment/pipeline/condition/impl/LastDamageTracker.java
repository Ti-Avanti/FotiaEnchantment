package gg.fotia.enchantment.pipeline.condition.impl;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家最近受伤时间追踪器
 * <p>由受伤监听器在 EntityDamageEvent 中调用 {@link #recordDamage(UUID)} 维护，
 * 供 {@link InCombatCondition}、{@link NotAttackedForCondition} 等条件查询。
 */
public final class LastDamageTracker {

    private static final Map<UUID, Long> LAST_DAMAGE = new ConcurrentHashMap<>();

    private LastDamageTracker() {
    }

    /** 记录玩家本次受伤时刻（毫秒） */
    public static void recordDamage(UUID uid) {
        if (uid == null) return;
        LAST_DAMAGE.put(uid, System.currentTimeMillis());
    }

    /** 获取最近一次受伤时刻；不存在时返回 null */
    public static Long getLastDamageTime(UUID uid) {
        if (uid == null) return null;
        return LAST_DAMAGE.get(uid);
    }

    /** 清除玩家追踪数据（玩家退出时调用） */
    public static void clear(UUID uid) {
        if (uid == null) return;
        LAST_DAMAGE.remove(uid);
    }

    public static void clearAll() {
        LAST_DAMAGE.clear();
    }
}
