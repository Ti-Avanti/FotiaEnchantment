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
    private final String enchantId;
    private Map<String, Double> variablesView;

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
        this(plugin, triggerContext, config, enchantLevel, variables, null);
    }

    public ConditionContext(FotiaEnchantment plugin,
                            TriggerContext triggerContext,
                            EnchantmentData.ConditionConfig config,
                            int enchantLevel,
                            Map<String, Double> variables,
                            String enchantId) {
        this.plugin = plugin;
        this.triggerContext = triggerContext;
        this.config = config;
        this.enchantLevel = enchantLevel;
        // 管道内部约定变量 Map 只读共享, 不做防御性拷贝 (一次执行可创建数十个上下文)
        this.variables = variables == null ? new HashMap<>() : variables;
        this.enchantId = enchantId;
    }

    public FotiaEnchantment getPlugin() {
        return plugin;
    }

    /**
     * 当前条件所属的附魔ID (可能为 null, 供需要按附魔隔离状态的条件使用)
     */
    public String getEnchantId() {
        return enchantId;
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
        if (variablesView == null) {
            variablesView = Collections.unmodifiableMap(variables);
        }
        return variablesView;
    }

    /**
     * 便捷方法：使用当前变量解析表达式
     */
    public double evaluateExpression(String expression) {
        return ExpressionParser.evaluate(expression, variables);
    }
}
