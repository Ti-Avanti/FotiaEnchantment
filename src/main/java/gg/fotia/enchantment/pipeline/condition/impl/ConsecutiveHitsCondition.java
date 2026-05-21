package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对同一目标连续命中条件 - 攻击者对同一目标连续命中数 ≥ 指定值
 * <p>由攻击监听器调用 {@link #recordHit(UUID, UUID)} 维护，目标变化或长时间未攻击应调用
 * {@link #reset(UUID)}。
 */
public class ConsecutiveHitsCondition implements Condition {

    private static final Map<UUID, Entry> HITS = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return "consecutive_hits";
    }

    @Override
    public boolean check(ConditionContext context) {
        Player player = context.getTriggerContext().getPlayer();
        LivingEntity target = context.getTriggerContext().getTarget();
        if (player == null || target == null) {
            return false;
        }
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        if (cfg == null) {
            return false;
        }
        double threshold = context.evaluateExpression(cfg.getString("value", "0"));
        Entry e = HITS.get(player.getUniqueId());
        if (e == null || !target.getUniqueId().equals(e.targetId)) {
            return false;
        }
        return e.count >= threshold;
    }

    /** 记录一次命中（同目标累加，不同目标重置） */
    public static void recordHit(UUID attacker, UUID target) {
        if (attacker == null || target == null) return;
        HITS.compute(attacker, (k, prev) -> {
            if (prev == null || !target.equals(prev.targetId)) {
                return new Entry(target, 1);
            }
            return new Entry(target, prev.count + 1);
        });
    }

    /** 重置玩家命中链 */
    public static void reset(UUID uid) {
        if (uid == null) return;
        HITS.remove(uid);
    }

    private static final class Entry {
        final UUID targetId;
        final int count;

        Entry(UUID targetId, int count) {
            this.targetId = targetId;
            this.count = count;
        }
    }
}
