package gg.fotia.enchantment.pipeline;

import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.util.ExpressionParser;

import java.util.Map;
import java.util.regex.Pattern;

public final class LevelCooldownPolicy {

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
                return Math.max(0L, Math.round(ExpressionParser.evaluate(normalizeFormula(formula, variables), variables)));
            } catch (RuntimeException ignored) {
                // Fall back to the legacy fixed cooldown below.
            }
        }

        return Math.max(0L, block.getCooldown());
    }

    private static String normalizeFormula(String formula, Map<String, Double> variables) {
        if (formula == null || variables == null || variables.isEmpty()) {
            return formula;
        }
        String result = formula;
        for (String key : variables.keySet()) {
            result = result.replaceAll(
                    "(?<![A-Za-z0-9_{])" + Pattern.quote(key) + "(?![A-Za-z0-9_}])",
                    "{" + key + "}");
        }
        return result;
    }
}
