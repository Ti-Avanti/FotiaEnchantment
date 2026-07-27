package gg.fotia.enchantment.pipeline;

import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.util.ExpressionParser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class LevelCooldownPolicy {

    /** 归一化结果按公式缓存; 变量键固定为 level/value/alt_value, 结果恒定 */
    private static final int FORMULA_CACHE_MAX = 256;
    private static final Map<String, String> FORMULA_CACHE = new ConcurrentHashMap<>();

    /** 预编译的变量包裹模式 (alt_value 必须先于 value 处理) */
    private static final Pattern[] KEY_PATTERNS = {
            Pattern.compile("(?<![A-Za-z0-9_{])alt_value(?![A-Za-z0-9_}])"),
            Pattern.compile("(?<![A-Za-z0-9_{])level(?![A-Za-z0-9_}])"),
            Pattern.compile("(?<![A-Za-z0-9_{])value(?![A-Za-z0-9_}])")
    };
    private static final String[] KEY_REPLACEMENTS = {"{alt_value}", "{level}", "{value}"};

    private LevelCooldownPolicy() {
    }

    public static long resolveCooldownTicks(EnchantmentData.EffectBlock block,
                                            int level,
                                            Map<String, Double> variables) {
        if (block == null) {
            return 0L;
        }

        Integer levelTicks = block.getCooldownLevels().get(level);
        if (levelTicks != null) {
            return Math.max(0L, levelTicks);
        }

        String formula = block.getCooldownFormula();
        if (formula != null && !formula.isBlank()) {
            try {
                return Math.max(0L, Math.round(ExpressionParser.evaluate(normalizeFormula(formula), variables)));
            } catch (RuntimeException ignored) {
                // Fall back to the legacy fixed cooldown below.
            }
        }

        return Math.max(0L, block.getCooldown());
    }

    private static String normalizeFormula(String formula) {
        String cached = FORMULA_CACHE.get(formula);
        if (cached != null) {
            return cached;
        }
        String result = formula;
        for (int i = 0; i < KEY_PATTERNS.length; i++) {
            result = KEY_PATTERNS[i].matcher(result).replaceAll(KEY_REPLACEMENTS[i]);
        }
        if (FORMULA_CACHE.size() >= FORMULA_CACHE_MAX) {
            FORMULA_CACHE.clear();
        }
        FORMULA_CACHE.put(formula, result);
        return result;
    }
}
