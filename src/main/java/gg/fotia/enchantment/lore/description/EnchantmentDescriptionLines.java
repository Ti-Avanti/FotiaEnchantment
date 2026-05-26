package gg.fotia.enchantment.lore.description;

import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.util.ExpressionParser;
import gg.fotia.enchantment.util.ExpressionPredicate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EnchantmentDescriptionLines {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([A-Za-z0-9_.-]+)}");

    private EnchantmentDescriptionLines() {
    }

    public static List<String> customDescriptionOrGenerated(
            List<String> configuredDescription,
            EnchantmentData data,
            int level,
            Function<String, String> guiLanguageResolver,
            String missingText
    ) {
        List<String> configured = nonBlankLines(configuredDescription);
        if (!configured.isEmpty()) {
            return renderConfigured(configured, data, level);
        }

        if (data != null) {
            List<String> generated = EnchantmentEffectDescriptionFormatter.renderLines(
                    data,
                    level,
                    guiLanguageResolver
            );
            if (!generated.isEmpty()) {
                return generated;
            }
        }

        return List.of(missingText);
    }

    private static List<String> renderConfigured(List<String> lines, EnchantmentData data, int level) {
        Map<String, String> placeholders = descriptionPlaceholders(data, level);
        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(renderLine(line, placeholders));
        }
        return List.copyOf(result);
    }

    private static Map<String, String> descriptionPlaceholders(EnchantmentData data, int level) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        putPlaceholder(placeholders, "level", String.valueOf(level));
        if (data == null || data.getEffects().isEmpty()) {
            return placeholders;
        }

        for (EnchantmentData.EffectBlock block : data.getEffects()) {
            if (!blockAppliesToLevel(block, level)) {
                continue;
            }
            collectConditionPlaceholders(placeholders, block, level);
            collectActionPlaceholders(placeholders, block, level);
        }
        return placeholders;
    }

    private static void collectConditionPlaceholders(
            Map<String, String> placeholders,
            EnchantmentData.EffectBlock block,
            int level
    ) {
        for (EnchantmentData.ConditionConfig condition : block.getConditions()) {
            String type = condition.getType() == null ? "" : condition.getType().toLowerCase(Locale.ROOT);
            if ("chance".equals(type)) {
                putPlaceholder(placeholders, "chance", evaluatedValue(condition.getValue(), level));
            }
            for (Map.Entry<String, Object> entry : condition.getExtraParamsView().entrySet()) {
                putPlaceholder(placeholders, entry.getKey(), evaluatedValue(entry.getValue(), level));
            }
        }
    }

    private static void collectActionPlaceholders(
            Map<String, String> placeholders,
            EnchantmentData.EffectBlock block,
            int level
    ) {
        for (EnchantmentData.ActionConfig action : block.getActions()) {
            String actionValue = null;
            if (action.getValue() != null && !action.getValue().isBlank()) {
                actionValue = evaluatedValue(action.getValue(), level);
                putPlaceholder(placeholders, "value", actionValue);
            }

            for (Map.Entry<String, Object> entry : action.getExtraParamsView().entrySet()) {
                putPlaceholder(placeholders, entry.getKey(), evaluatedValue(entry.getValue(), level));
            }

            putActionAliases(placeholders, action, actionValue, level);
            putDerivedAliases(placeholders);
        }
    }

    private static void putActionAliases(
            Map<String, String> placeholders,
            EnchantmentData.ActionConfig action,
            String actionValue,
            int level
    ) {
        String type = action.getType() == null ? "" : action.getType().toUpperCase(Locale.ROOT);
        switch (type) {
            case "DAMAGE_ADD", "TRUE_DAMAGE" -> {
                    putPlaceholder(placeholders, "amount", actionValue);
                    putPlaceholder(placeholders, "damage", actionValue);
            }
            case "HEAL" -> {
                    putPlaceholder(placeholders, "amount", actionValue);
                    putPercentFromActionValue(placeholders, actionValue, 5.0);
            }
            case "HELD_ITEM_REPAIR" ->
                    putPlaceholder(placeholders, "amount", actionValue);
            case "DAMAGE_REDUCE", "LIFESTEAL", "THORNS" ->
                    putPlaceholder(placeholders, "percent", actionValue);
            case "DAMAGE_MULTIPLY", "BONUS_DROP" ->
                    putPlaceholder(placeholders, "multiplier", actionValue);
            case "LAUNCH" ->
                    putPlaceholder(placeholders, "power", actionValue);
            case "SPEED_BOOST" ->
                    putDisplayedLevelFromActionValue(placeholders, "amplifier", actionValue);
            case "IGNITE_TARGET" ->
                    putSecondsFromActionValue(placeholders, actionValue);
            case "ADD_POTION_SELF", "ADD_POTION_TARGET" ->
                    putPotionAliases(placeholders, action, level);
            default -> {
                putPlaceholder(placeholders, "amount", actionValue);
                putPlaceholder(placeholders, "damage", actionValue);
            }
        }
    }

    private static void putPercentFromActionValue(
            Map<String, String> placeholders,
            String actionValue,
            double multiplier
    ) {
        Double value = parseNumber(actionValue);
        if (value != null) {
            putPlaceholder(placeholders, "percent", EnchantmentEffectDescriptionFormatter.number(value * multiplier));
        }
    }

    private static void putDisplayedLevelFromActionValue(
            Map<String, String> placeholders,
            String key,
            String actionValue
    ) {
        Double value = parseNumber(actionValue);
        if (value != null) {
            putPlaceholder(placeholders, key, EnchantmentEffectDescriptionFormatter.number(value + 1.0));
        }
    }

    private static void putSecondsFromActionValue(Map<String, String> placeholders, String actionValue) {
        Double ticks = parseNumber(actionValue);
        if (ticks != null) {
            putPlaceholder(placeholders, "duration", EnchantmentEffectDescriptionFormatter.number(ticks));
            putPlaceholder(placeholders, "seconds", EnchantmentEffectDescriptionFormatter.number(ticks / 20.0));
        }
    }

    private static void putPotionAliases(
            Map<String, String> placeholders,
            EnchantmentData.ActionConfig action,
            int level
    ) {
        Object rawAmplifier = action.getExtraParamsView().get("amplifier");
        if (rawAmplifier == null) {
            return;
        }

        Double amplifier = parseNumber(evaluatedValue(rawAmplifier, level));
        if (amplifier != null) {
            setPlaceholder(placeholders, "amplifier", EnchantmentEffectDescriptionFormatter.number(amplifier + 1.0));
        }
    }

    private static void putDerivedAliases(Map<String, String> placeholders) {
        String duration = firstPresent(placeholders, "duration");
        if (duration != null) {
            Double ticks = parseNumber(duration);
            if (ticks != null) {
                putPlaceholder(placeholders, "seconds", EnchantmentEffectDescriptionFormatter.number(ticks / 20.0));
            }
        }

        String maxBlocks = firstPresent(placeholders, "max-blocks", "max_blocks");
        if (maxBlocks != null) {
            putPlaceholder(placeholders, "blocks", maxBlocks);
            putPlaceholder(placeholders, "radius", maxBlocks);
        }
    }

    private static String firstPresent(Map<String, String> placeholders, String... keys) {
        for (String key : keys) {
            String value = placeholders.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static boolean blockAppliesToLevel(EnchantmentData.EffectBlock block, int level) {
        for (EnchantmentData.ConditionConfig condition : block.getConditions()) {
            String type = condition.getType() == null ? "" : condition.getType().toLowerCase(Locale.ROOT);
            if (!type.equals("expression_true") && !type.equals("expression_false")) {
                continue;
            }

            String expression = condition.getString("expression", condition.getValue());
            if (expression == null || expression.isBlank()) {
                continue;
            }

            try {
                boolean matched = ExpressionPredicate.evaluate(expression, Map.of("level", (double) level));
                if (type.equals("expression_false")) {
                    matched = !matched;
                }
                if (!matched) {
                    return false;
                }
            } catch (RuntimeException ignored) {
                // Runtime-only expressions may depend on trigger values; keep them visible in static lore.
            }
        }
        return true;
    }

    private static String evaluatedValue(Object raw, int level) {
        if (raw == null) {
            return "";
        }
        if (raw instanceof Number number) {
            return EnchantmentEffectDescriptionFormatter.number(number.doubleValue());
        }
        if (raw instanceof Boolean bool) {
            return String.valueOf(bool);
        }

        String text = String.valueOf(raw).trim();
        if (text.isBlank()) {
            return "";
        }

        try {
            return EnchantmentEffectDescriptionFormatter.number(
                    ExpressionParser.evaluate(text, Map.of("level", (double) level))
            );
        } catch (RuntimeException ignored) {
            return text.replace("{level}", String.valueOf(level));
        }
    }

    private static Double parseNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void putPlaceholder(Map<String, String> placeholders, String key, String value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        placeholders.putIfAbsent(normalized, value);
        placeholders.putIfAbsent(normalized.replace('-', '_'), value);
        placeholders.putIfAbsent(normalized.replace('_', '-'), value);
    }

    private static void setPlaceholder(Map<String, String> placeholders, String key, String value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        placeholders.put(normalized, value);
        placeholders.put(normalized.replace('-', '_'), value);
        placeholders.put(normalized.replace('_', '-'), value);
    }

    private static String renderLine(String raw, Map<String, String> placeholders) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(raw);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase(Locale.ROOT);
            String value = placeholders.get(key);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? matcher.group(0) : value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static List<String> nonBlankLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                result.add(line);
            }
        }
        return List.copyOf(result);
    }
}
