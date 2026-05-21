package gg.fotia.enchantment.lore;

import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.util.ExpressionParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class EnchantmentEffectDescriptionFormatter {

    public record LevelSummary(int level, String chance, List<Phrase> phrases) {
    }

    public record Phrase(String key, Map<String, String> placeholders) {
    }

    private EnchantmentEffectDescriptionFormatter() {
    }

    public static List<String> renderLines(
            EnchantmentData data,
            int level,
            Function<String, String> languageResolver
    ) {
        return renderLines(data, List.of(level), languageResolver);
    }

    public static List<String> renderLines(
            EnchantmentData data,
            List<Integer> levels,
            Function<String, String> languageResolver
    ) {
        List<String> lines = new ArrayList<>();
        for (LevelSummary summary : buildSummaries(data, levels)) {
            lines.add(renderSummary(summary, languageResolver));
        }
        return List.copyOf(lines);
    }

    public static List<LevelSummary> buildSummaries(EnchantmentData data, List<Integer> levels) {
        if (data == null || data.getEffects().isEmpty() || levels == null || levels.isEmpty()) {
            return List.of();
        }

        List<LevelSummary> summaries = new ArrayList<>();
        for (int level : levels) {
            for (EnchantmentData.EffectBlock block : data.getEffects()) {
                List<Phrase> phrases = actionPhrases(block, level);
                if (phrases.isEmpty()) {
                    continue;
                }
                summaries.add(new LevelSummary(level, chance(block, level), List.copyOf(phrases)));
            }
        }
        return List.copyOf(summaries);
    }

    private static String renderSummary(LevelSummary summary, Function<String, String> languageResolver) {
        List<String> effectParts = new ArrayList<>();
        for (Phrase phrase : summary.phrases()) {
            effectParts.add(renderPhrase(phrase, languageResolver));
        }

        Map<String, String> placeholders = Map.of(
                "level", String.valueOf(summary.level()),
                "chance", summary.chance(),
                "chance_phrase", summary.chance().isBlank()
                        ? ""
                        : render(lang(languageResolver, "guide-gui.detail-chance"), Map.of("chance", summary.chance())),
                "effects", String.join(lang(languageResolver, "guide-gui.detail-joiner"), effectParts)
        );
        return render(lang(languageResolver, "guide-gui.detail-line"), placeholders);
    }

    private static String renderPhrase(Phrase phrase, Function<String, String> languageResolver) {
        Map<String, String> placeholders = new LinkedHashMap<>(phrase.placeholders());
        if (placeholders.containsKey("potion")) {
            placeholders.put("potion", localizePotion(placeholders.get("potion"), languageResolver));
        }

        String key = "guide-gui.effect-phrase-" + phrase.key();
        String template = lang(languageResolver, key);
        if (key.equals(template)) {
            return humanizeId(phrase.key());
        }
        return render(template, placeholders);
    }

    private static String lang(Function<String, String> languageResolver, String key) {
        if (languageResolver == null) {
            return key;
        }
        String value = languageResolver.apply(key);
        return value == null ? key : value;
    }

    private static String render(String raw, Map<String, String> placeholders) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String result = raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private static String localizePotion(String potion, Function<String, String> languageResolver) {
        if (potion == null || potion.isBlank()) {
            return "";
        }

        String normalized = potion.toUpperCase(Locale.ROOT);
        String key = "guide-gui.potion-" + normalized;
        String localized = lang(languageResolver, key);
        return key.equals(localized) ? humanizeId(normalized) : localized;
    }

    private static String humanizeId(String id) {
        String[] parts = id.toLowerCase(Locale.ROOT).split("_+");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
        }
        return String.join(" ", words);
    }

    private static String chance(EnchantmentData.EffectBlock block, int level) {
        for (EnchantmentData.ConditionConfig condition : block.getConditions()) {
            if ("chance".equalsIgnoreCase(condition.getType())) {
                return number(evaluate(condition.getValue(), level));
            }
        }
        return "";
    }

    private static List<Phrase> actionPhrases(EnchantmentData.EffectBlock block, int level) {
        List<Phrase> phrases = new ArrayList<>();
        for (EnchantmentData.ActionConfig action : block.getActions()) {
            String type = action.getType() == null ? "" : action.getType().toUpperCase(Locale.ROOT);
            Phrase phrase = switch (type) {
                case "HEAL" -> phrase("HEAL", Map.of(
                        "amount", number(value(action, level)),
                        "percent", number(value(action, level) * 5.0)
                ));
                case "ADD_POTION_SELF" -> potionPhrase("ADD_POTION_SELF", action, level);
                case "ADD_POTION_TARGET" -> potionPhrase("ADD_POTION_TARGET", action, level);
                case "DAMAGE_ADD" -> phrase("DAMAGE_ADD", Map.of("amount", number(value(action, level))));
                case "TRUE_DAMAGE" -> phrase("TRUE_DAMAGE", Map.of("amount", number(value(action, level))));
                case "DAMAGE_REDUCE" -> phrase("DAMAGE_REDUCE", Map.of("percent", number(value(action, level))));
                case "DAMAGE_MULTIPLY" -> phrase("DAMAGE_MULTIPLY", Map.of("multiplier", number(value(action, level))));
                case "LIFESTEAL" -> phrase("LIFESTEAL", Map.of("percent", number(value(action, level))));
                case "IGNITE_TARGET" -> phrase("IGNITE_TARGET", Map.of("seconds", ticks(action, level)));
                case "THORNS" -> phrase("THORNS", Map.of("percent", number(value(action, level))));
                case "BONUS_DROP" -> phrase("BONUS_DROP", Map.of("multiplier", number(value(action, level))));
                case "LAUNCH" -> phrase("LAUNCH", Map.of("power", number(value(action, level))));
                case "LIGHTNING" -> phrase("LIGHTNING", Map.of());
                case "EXPLODE" -> phrase("EXPLODE", Map.of("power", number(param(action, "power", level))));
                case "SPEED_BOOST" -> phrase("SPEED_BOOST", Map.of(
                        "amplifier", number(value(action, level) + 1),
                        "seconds", ticks(action, level, 100.0)
                ));
                case "SMELT" -> phrase("SMELT", Map.of());
                case "VEIN_MINE" -> phrase("VEIN_MINE", Map.of("radius", number(param(action, "radius", level))));
                case "HELD_ITEM_REPAIR" -> phrase("HELD_ITEM_REPAIR", Map.of("amount", number(value(action, level))));
                default -> null;
            };
            if (phrase != null) {
                phrases.add(phrase);
            }
        }
        return phrases;
    }

    private static Phrase potionPhrase(String key, EnchantmentData.ActionConfig action, int level) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("potion", stringParam(action, "potion", ""));
        placeholders.put("seconds", ticks(action, level));
        placeholders.put("amplifier", number(param(action, "amplifier", level) + 1));
        return phrase(key, placeholders);
    }

    private static Phrase phrase(String key, Map<String, String> placeholders) {
        return new Phrase(key, Map.copyOf(placeholders));
    }

    private static double value(EnchantmentData.ActionConfig action, int level) {
        return evaluate(action.getValue(), level);
    }

    private static double param(EnchantmentData.ActionConfig action, String key, int level) {
        Object raw = action.getExtraParamsView().get(key);
        if (raw == null) {
            return 0.0;
        }
        return evaluate(String.valueOf(raw), level);
    }

    private static String stringParam(EnchantmentData.ActionConfig action, String key, String fallback) {
        Object raw = action.getExtraParamsView().get(key);
        return raw == null ? fallback : String.valueOf(raw);
    }

    private static String ticks(EnchantmentData.ActionConfig action, int level) {
        double ticks = param(action, "duration", level);
        if (ticks <= 0) {
            ticks = value(action, level);
        }
        return number(ticks / 20.0);
    }

    private static String ticks(EnchantmentData.ActionConfig action, int level, double fallbackTicks) {
        double ticks = param(action, "duration", level);
        if (ticks <= 0) {
            ticks = fallbackTicks;
        }
        return number(ticks / 20.0);
    }

    private static double evaluate(String expression, int level) {
        if (expression == null || expression.isBlank()) {
            return 0.0;
        }
        try {
            return ExpressionParser.evaluate(expression, Map.of("level", (double) level));
        } catch (RuntimeException ignored) {
            return 0.0;
        }
    }

    public static String number(double value) {
        double rounded = Math.round(value * 10.0) / 10.0;
        if (Math.abs(rounded - Math.rint(rounded)) < 0.0000001) {
            return String.valueOf((long) Math.rint(rounded));
        }
        return String.format(Locale.ROOT, "%.1f", rounded);
    }
}
