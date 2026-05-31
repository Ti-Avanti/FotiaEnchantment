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
        Map<String, Integer> placeholderIndexes = new LinkedHashMap<>();
        putPlaceholder(placeholders, "level", String.valueOf(level));
        if (data == null || data.getEffects().isEmpty()) {
            return placeholders;
        }

        for (EnchantmentData.EffectBlock block : data.getEffects()) {
            if (!blockAppliesToLevel(block, level)) {
                continue;
            }
            collectEffectBlockPlaceholders(placeholders, placeholderIndexes, block);
            collectConditionPlaceholders(placeholders, placeholderIndexes, block, level);
            collectActionPlaceholders(placeholders, placeholderIndexes, block, level);
        }
        return placeholders;
    }

    private static void collectEffectBlockPlaceholders(
            Map<String, String> placeholders,
            Map<String, Integer> placeholderIndexes,
            EnchantmentData.EffectBlock block
    ) {
        int cooldown = block.getCooldown();
        if (cooldown <= 0) {
            return;
        }
        String ticks = EnchantmentEffectDescriptionFormatter.number(cooldown);
        String seconds = EnchantmentEffectDescriptionFormatter.number(cooldown / 20.0);
        putNumberedPlaceholder(placeholders, placeholderIndexes, "cooldown", ticks);
        putNumberedPlaceholder(placeholders, placeholderIndexes, "cooldown-ticks", ticks);
        putNumberedPlaceholder(placeholders, placeholderIndexes, "cooldown-seconds", seconds);
    }

    private static void collectConditionPlaceholders(
            Map<String, String> placeholders,
            Map<String, Integer> placeholderIndexes,
            EnchantmentData.EffectBlock block,
            int level
    ) {
        for (EnchantmentData.ConditionConfig condition : block.getConditions()) {
            String type = condition.getType() == null ? "" : condition.getType().toLowerCase(Locale.ROOT);
            if ("chance".equals(type)) {
                putNumberedPlaceholder(placeholders, placeholderIndexes, "chance",
                        evaluatedValue(condition.getValue(), level));
            }
            for (Map.Entry<String, Object> entry : condition.getExtraParamsView().entrySet()) {
                putNumberedPlaceholder(placeholders, placeholderIndexes, entry.getKey(),
                        evaluatedValue(entry.getValue(), level));
            }
        }
    }

    private static void collectActionPlaceholders(
            Map<String, String> placeholders,
            Map<String, Integer> placeholderIndexes,
            EnchantmentData.EffectBlock block,
            int level
    ) {
        for (EnchantmentData.ActionConfig action : block.getActions()) {
            String type = action.getType() == null ? "" : action.getType().toUpperCase(Locale.ROOT);
            String actionValue = null;
            if (action.getValue() != null && !action.getValue().isBlank()) {
                actionValue = evaluatedValue(action.getValue(), level);
                putNumberedPlaceholder(placeholders, placeholderIndexes, "value", actionValue);
            }

            for (Map.Entry<String, Object> entry : action.getExtraParamsView().entrySet()) {
                String key = entry.getKey();
                if (isPotionAction(type) && "amplifier".equals(normalizedKey(key))) {
                    continue;
                }
                putActionParamPlaceholder(placeholders, placeholderIndexes, key,
                        evaluatedValue(entry.getValue(), level));
            }

            putActionAliases(placeholders, placeholderIndexes, action, actionValue, level);
        }
    }

    private static void putActionAliases(
            Map<String, String> placeholders,
            Map<String, Integer> placeholderIndexes,
            EnchantmentData.ActionConfig action,
            String actionValue,
            int level
    ) {
        String type = action.getType() == null ? "" : action.getType().toUpperCase(Locale.ROOT);
        switch (type) {
            case "DAMAGE_ADD", "TRUE_DAMAGE" -> {
                    putNumberedPlaceholder(placeholders, placeholderIndexes, "amount", actionValue);
                    putNumberedPlaceholder(placeholders, placeholderIndexes, "damage", actionValue);
            }
            case "HEAL" -> {
                    putNumberedPlaceholder(placeholders, placeholderIndexes, "amount", actionValue);
                    putPercentFromActionValue(placeholders, placeholderIndexes, actionValue, 5.0);
            }
            case "HELD_ITEM_REPAIR" ->
                    putNumberedPlaceholder(placeholders, placeholderIndexes, "amount", actionValue);
            case "DAMAGE_REDUCE", "LIFESTEAL", "THORNS" ->
                    putNumberedPlaceholder(placeholders, placeholderIndexes, "percent", actionValue);
            case "DAMAGE_MULTIPLY", "BONUS_DROP" ->
                    putNumberedPlaceholder(placeholders, placeholderIndexes, "multiplier", actionValue);
            case "LAUNCH" ->
                    putNumberedPlaceholder(placeholders, placeholderIndexes, "power", actionValue);
            case "SPEED_BOOST" ->
                    putDisplayedLevelFromActionValue(placeholders, placeholderIndexes, "amplifier", actionValue);
            case "IGNITE_TARGET" ->
                    putDurationPlaceholders(placeholders, placeholderIndexes, actionValue);
            case "ADD_POTION_SELF", "ADD_POTION_TARGET" ->
                    putPotionAliases(placeholders, placeholderIndexes, action, level);
            default -> {
                putNumberedPlaceholder(placeholders, placeholderIndexes, "amount", actionValue);
                putNumberedPlaceholder(placeholders, placeholderIndexes, "damage", actionValue);
            }
        }
    }

    private static void putActionParamPlaceholder(
            Map<String, String> placeholders,
            Map<String, Integer> placeholderIndexes,
            String key,
            String value
    ) {
        String normalized = normalizedKey(key);
        if ("duration".equals(normalized)) {
            putDurationPlaceholders(placeholders, placeholderIndexes, value);
            return;
        }

        putNumberedPlaceholder(placeholders, placeholderIndexes, key, value);
        if ("max-blocks".equals(normalized.replace('_', '-'))) {
            putNumberedPlaceholder(placeholders, placeholderIndexes, "blocks", value);
            putNumberedPlaceholder(placeholders, placeholderIndexes, "radius", value);
        }
    }

    private static void putPercentFromActionValue(
            Map<String, String> placeholders,
            Map<String, Integer> placeholderIndexes,
            String actionValue,
            double multiplier
    ) {
        Double value = parseNumber(actionValue);
        if (value != null) {
            putNumberedPlaceholder(placeholders, placeholderIndexes, "percent",
                    EnchantmentEffectDescriptionFormatter.number(value * multiplier));
        }
    }

    private static void putDisplayedLevelFromActionValue(
            Map<String, String> placeholders,
            Map<String, Integer> placeholderIndexes,
            String key,
            String actionValue
    ) {
        Double value = parseNumber(actionValue);
        if (value != null) {
            putNumberedPlaceholder(placeholders, placeholderIndexes, key,
                    EnchantmentEffectDescriptionFormatter.number(value + 1.0));
        }
    }

    private static void putDurationPlaceholders(
            Map<String, String> placeholders,
            Map<String, Integer> placeholderIndexes,
            String ticksValue
    ) {
        putNumberedPlaceholder(placeholders, placeholderIndexes, "duration", ticksValue);
        Double ticks = parseNumber(ticksValue);
        if (ticks != null) {
            putNumberedPlaceholder(placeholders, placeholderIndexes, "seconds",
                    EnchantmentEffectDescriptionFormatter.number(ticks / 20.0));
        }
    }

    private static void putPotionAliases(
            Map<String, String> placeholders,
            Map<String, Integer> placeholderIndexes,
            EnchantmentData.ActionConfig action,
            int level
    ) {
        Object rawAmplifier = action.getExtraParamsView().get("amplifier");
        if (rawAmplifier == null) {
            return;
        }

        Double amplifier = parseNumber(evaluatedValue(rawAmplifier, level));
        if (amplifier != null) {
            putNumberedPlaceholder(placeholders, placeholderIndexes, "amplifier",
                    EnchantmentEffectDescriptionFormatter.number(amplifier + 1.0));
        }
    }

    private static boolean isPotionAction(String type) {
        return "ADD_POTION_SELF".equals(type) || "ADD_POTION_TARGET".equals(type);
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

    private static void putNumberedPlaceholder(
            Map<String, String> placeholders,
            Map<String, Integer> placeholderIndexes,
            String key,
            String value
    ) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        String normalized = normalizedKey(key);
        String indexKey = normalized.replace('_', '-');
        int index = placeholderIndexes.merge(indexKey, 1, Integer::sum);
        if (index == 1) {
            putPlaceholder(placeholders, normalized, value);
        }
        putPlaceholder(placeholders, normalized + index, value);
    }

    private static void putPlaceholder(Map<String, String> placeholders, String key, String value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        String normalized = normalizedKey(key);
        placeholders.putIfAbsent(normalized, value);
        placeholders.putIfAbsent(normalized.replace('-', '_'), value);
        placeholders.putIfAbsent(normalized.replace('_', '-'), value);
    }

    private static String normalizedKey(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
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
