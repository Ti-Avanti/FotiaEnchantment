package gg.fotia.enchantment.pipeline.effect;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.pipeline.trigger.TriggerContext;
import gg.fotia.enchantment.util.ExpressionParser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 效果执行上下文 - 携带动作配置参数与变量
 */
public class EffectContext {

    private final TriggerContext triggerContext;
    private final EnchantmentData.ActionConfig config;
    private final int enchantLevel;
    private final Map<String, Double> variables;
    private final FotiaEnchantment plugin;
    private boolean stopChain;

    public EffectContext(TriggerContext triggerContext,
                         EnchantmentData.ActionConfig config,
                         int enchantLevel,
                         Map<String, Double> variables) {
        this(null, triggerContext, config, enchantLevel, variables);
    }

    public EffectContext(FotiaEnchantment plugin,
                         TriggerContext triggerContext,
                         EnchantmentData.ActionConfig config,
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

    public void stopChain() {
        this.stopChain = true;
    }

    public boolean isStopChain() {
        return stopChain;
    }

    public TriggerContext getTriggerContext() {
        return triggerContext;
    }

    public EnchantmentData.ActionConfig getConfig() {
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

    /**
     * 获取配置中的 value 字段（字符串形式，可作为表达式）
     */
    public String getConfigValue() {
        return config == null ? null : config.getValue();
    }

    /**
     * 获取配置中指定额外参数（字符串形式）
     */
    public String getExtraParam(String key) {
        if (config == null) return null;
        Object raw = config.getExtraParams().get(key);
        return raw == null ? null : String.valueOf(raw);
    }

    /**
     * 获取配置中指定额外参数（带默认值）
     */
    public String getExtraParam(String key, String defaultValue) {
        String v = getExtraParam(key);
        return v == null ? defaultValue : v;
    }

    /**
     * 获取配置中指定字段并解析为 double（支持表达式与变量）
     */
    public double getDoubleParam(String key, double defaultValue) {
        String v = getExtraParam(key);
        if (v == null || v.isEmpty()) return defaultValue;
        try {
            return ExpressionParser.evaluate(v, variables);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    /**
     * 获取配置中指定字段并解析为 int
     */
    public int getIntParam(String key, int defaultValue) {
        return (int) getDoubleParam(key, defaultValue);
    }

    /**
     * 获取布尔配置参数
     */
    public boolean getBooleanParam(String key, boolean defaultValue) {
        if (config == null) return defaultValue;
        Object raw = config.getExtraParams().get(key);
        if (raw == null) return defaultValue;
        if (raw instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(raw));
    }
}
