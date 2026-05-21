package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;

import java.util.Random;

/**
 * 概率条件 - 按百分比概率判断是否满足
 * <p>支持表达式形式（如 "{level} * 10"），结果按百分比计算。
 */
public class ChanceCondition implements Condition {

    private static final Random RANDOM = new Random();

    @Override
    public String getId() {
        return "chance";
    }

    @Override
    public boolean check(ConditionContext context) {
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        if (cfg == null) {
            return true;
        }
        String valueStr = cfg.getString("value");
        if (valueStr == null || valueStr.isEmpty()) {
            return true;
        }
        double chance = context.evaluateExpression(valueStr);
        return RANDOM.nextDouble() * 100.0 < chance;
    }
}
