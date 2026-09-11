package gg.fotia.enchantment.pipeline;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 冷却管理器 - 管理附魔效果的冷却时间
 *
 * <p>冷却 key 通常为 {@code enchantId:effectIndex}，由调用方组装。
 * 内部以毫秒级时间戳保存过期时间，1 tick 视为 50ms。
 */
public class CooldownManager {

    /** 1 tick = 50 ms */
    private static final long MS_PER_TICK = 50L;

    /** key: 玩家UUID, value: Map&lt;冷却key, 过期时间戳(ms)&gt; */
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    /**
     * 检查是否在冷却中
     *
     * @param playerId 玩家UUID
     * @param key      冷却key（格式：enchantId:effectIndex）
     * @return true=在冷却中
     */
    public boolean isOnCooldown(UUID playerId, String key) {
        if (playerId == null || key == null) {
            return false;
        }
        Map<String, Long> playerMap = cooldowns.get(playerId);
        if (playerMap == null) {
            return false;
        }
        Long expireAt = playerMap.get(key);
        if (expireAt == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now >= expireAt) {
            // 过期则顺手清理
            cooldowns.computeIfPresent(playerId, (id, current) -> {
                current.remove(key, expireAt);
                return current.isEmpty() ? null : current;
            });
            return false;
        }
        return true;
    }

    /**
     * 获取剩余冷却时间（ticks）
     *
     * @param playerId 玩家UUID
     * @param key      冷却key
     * @return 剩余 ticks，若未在冷却中返回 0
     */
    public long getRemaining(UUID playerId, String key) {
        if (playerId == null || key == null) {
            return 0L;
        }
        Map<String, Long> playerMap = cooldowns.get(playerId);
        if (playerMap == null) {
            return 0L;
        }
        Long expireAt = playerMap.get(key);
        if (expireAt == null) {
            return 0L;
        }
        long remainingMs = expireAt - System.currentTimeMillis();
        if (remainingMs <= 0) {
            return 0L;
        }
        return (remainingMs + MS_PER_TICK - 1) / MS_PER_TICK;
    }

    /**
     * 设置冷却
     *
     * @param playerId 玩家UUID
     * @param key      冷却key
     * @param ticks    冷却时长(ticks)，&lt;=0 表示移除冷却
     */
    public void setCooldown(UUID playerId, String key, long ticks) {
        if (playerId == null || key == null) {
            return;
        }
        if (ticks <= 0) {
            cooldowns.computeIfPresent(playerId, (id, current) -> {
                current.remove(key);
                return current.isEmpty() ? null : current;
            });
            return;
        }
        long expireAt = System.currentTimeMillis() + ticks * MS_PER_TICK;
        cooldowns.compute(playerId, (id, current) -> {
            Map<String, Long> playerMap = current == null ? new ConcurrentHashMap<>() : current;
            playerMap.put(key, expireAt);
            return playerMap;
        });
    }

    /**
     * 清除玩家所有冷却（玩家退出时调用）
     */
    public void clearPlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }
        cooldowns.remove(playerId);
    }

    /**
     * 清除所有冷却数据
     */
    public void clearAll() {
        cooldowns.clear();
    }

    /**
     * 清理所有已过期的冷却记录（可由定期任务调用）
     */
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        for (UUID playerId : cooldowns.keySet()) {
            // 与设置冷却使用同一个玩家键上的原子更新，避免删掉刚写入的新冷却。
            cooldowns.computeIfPresent(playerId, (id, current) -> {
                current.entrySet().removeIf(entry -> entry.getValue() <= now);
                return current.isEmpty() ? null : current;
            });
        }
    }
}
