package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import gg.fotia.enchantment.core.EnchantmentData;
import org.bukkit.entity.Player;

/**
 * 战斗状态条件 - 玩家是否处于战斗中（5 秒内受伤视为战斗）
 */
public class InCombatCondition implements Condition {

    /** 战斗判定窗口（毫秒） */
    private static final long COMBAT_WINDOW_MS = 5000L;

    @Override
    public String getId() {
        return "in_combat";
    }

    @Override
    public boolean check(ConditionContext context) {
        Player player = context.getTriggerContext().getPlayer();
        if (player == null) {
            return false;
        }
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        boolean expected = cfg == null || cfg.getBoolean("value", true);

        // getLastDamage 是数值伤害；改用 getNoDamageTicks 估算
        // 在受伤后会进入无敌帧（默认 20tick），过后逐渐降低
        boolean inCombat = player.getNoDamageTicks() > 0
                || (System.currentTimeMillis() - getLastDamageTime(player)) < COMBAT_WINDOW_MS;
        return inCombat == expected;
    }

    private long getLastDamageTime(Player player) {
        // 通过 metadata 兜底；若未由监听器维护，则视为很久之前
        Long t = LastDamageTracker.getLastDamageTime(player.getUniqueId());
        return t == null ? 0L : t;
    }
}
