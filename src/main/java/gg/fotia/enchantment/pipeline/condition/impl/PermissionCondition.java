package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import gg.fotia.enchantment.core.EnchantmentData;
import org.bukkit.entity.Player;

/**
 * 权限条件 - 检查玩家是否拥有指定权限节点
 */
public class PermissionCondition implements Condition {

    @Override
    public String getId() {
        return "permission";
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
        String node = cfg.getString("value");
        if (node == null || node.isEmpty()) {
            return true;
        }
        return player.hasPermission(node);
    }
}
