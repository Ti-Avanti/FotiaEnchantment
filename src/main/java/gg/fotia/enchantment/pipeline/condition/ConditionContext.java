package gg.fotia.enchantment.pipeline.condition;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import gg.fotia.enchantment.util.ExpressionParser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 条件上下文 - 继承触发上下文信息，并附加该条件块的配置参数
 */
public class ConditionContext {

    private final TriggerContext triggerContext;
    private final EnchantmentData.ConditionConfig config;
    private final int enchantLevel;
    private final Map<String, Double> variables;
    private final FotiaEnchantment plugin;

    public ConditionContext(TriggerContext triggerContext,
                            EnchantmentData.ConditionConfig config,
                            int enchantLevel,
                            Map<String, Double> variables) {
        this(null, triggerContext, config, enchantLevel, variables);
    }

    public ConditionContext(FotiaEnchantment plugin,
                            TriggerContext triggerContext,
                            EnchantmentData.ConditionConfig config,
                            int enchantLevel,
                            Map<String, Double> variables) {
        this.plugin = plugin;
        this.triggerContext = triggerContext;
        this.config = config;
        this.enchantLevel = enchantLevel;
        this.variables = variables == null
                ? new HashMap<>()
                : new HashMap<>(variables);
    }

    public FotiaEnchantment getPlugin() {
        return plugin;
    }

    public TriggerContext getTriggerContext() {
        return triggerContext;
    }

    public EnchantmentData.ConditionConfig getConfig() {
        return config;
    }

    public int getEnchantLevel() {
        return enchantLevel;
    }

    public Map<String, Double> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    /**
     * 便捷方法：使用当前变量解析表达式
     */
    public double evaluateExpression(String expression) {
        return ExpressionParser.evaluate(expression, variables);
    }
}
