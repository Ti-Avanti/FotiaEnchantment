package gg.fotia.enchantment.gui.guide;

import gg.fotia.enchantment.core.EnchantmentData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

final class EnchantmentGuideEffectFormatter {

    enum LineType {
        TRIGGER,
        COOLDOWN,
        CONDITION,
        ACTION
    }

    record DetailLine(LineType type, int effectIndex, String name, String parameters) {
    }

    private EnchantmentGuideEffectFormatter() {
    }

    static List<DetailLine> buildLines(
            EnchantmentData data,
            Function<String, String> triggerNames,
            Function<String, String> conditionNames,
            Function<String, String> actionNames
    ) {
        if (data == null || data.getEffects().isEmpty()) {
            return List.of();
        }

        List<DetailLine> lines = new ArrayList<>();
        int index = 1;
        for (EnchantmentData.EffectBlock block : data.getEffects()) {
            String trigger = block.getTrigger() == null ? "" : block.getTrigger();
            lines.add(new DetailLine(LineType.TRIGGER, index, triggerNames.apply(trigger), ""));

            if (block.getCooldown() > 0) {
                lines.add(new DetailLine(LineType.COOLDOWN, index, "cooldown", block.getCooldown() + " ticks"));
            }

            for (EnchantmentData.ConditionConfig condition : block.getConditions()) {
                String type = condition.getType() == null ? "" : condition.getType();
                lines.add(new DetailLine(
                        LineType.CONDITION,
                        index,
                        conditionNames.apply(type),
                        formatParameters(condition.getValue(), condition.getExtraParamsView())
                ));
            }

            for (EnchantmentData.ActionConfig action : block.getActions()) {
                String type = action.getType() == null ? "" : action.getType();
                lines.add(new DetailLine(
                        LineType.ACTION,
                        index,
                        actionNames.apply(type),
                        formatParameters(action.getValue(), action.getExtraParamsView())
                ));
            }
            index++;
        }
        return List.copyOf(lines);
    }

    static List<DetailLine> buildActionLines(
            EnchantmentData data,
            Function<String, String> actionNames
    ) {
        if (data == null || data.getEffects().isEmpty()) {
            return List.of();
        }

        List<DetailLine> lines = new ArrayList<>();
        int index = 1;
        for (EnchantmentData.EffectBlock block : data.getEffects()) {
            for (EnchantmentData.ActionConfig action : block.getActions()) {
                String type = action.getType() == null ? "" : action.getType();
                lines.add(new DetailLine(
                        LineType.ACTION,
                        index,
                        actionNames.apply(type),
                        formatParameters(action.getValue(), action.getExtraParamsView())
                ));
            }
            index++;
        }
        return List.copyOf(lines);
    }

    private static String formatParameters(String value, Map<String, Object> extraParams) {
        StringJoiner joiner = new StringJoiner(", ");
        if (value != null && !value.isBlank()) {
            joiner.add("value=" + value);
        }
        extraParams.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> joiner.add(entry.getKey() + "=" + stringify(entry.getValue())));
        return joiner.toString();
    }

    private static String stringify(Object value) {
        if (value instanceof Iterable<?> iterable) {
            StringJoiner joiner = new StringJoiner(", ", "[", "]");
            for (Object item : iterable) {
                joiner.add(String.valueOf(item));
            }
            return joiner.toString();
        }
        return String.valueOf(value);
    }
}
