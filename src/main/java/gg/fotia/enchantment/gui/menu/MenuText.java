package gg.fotia.enchantment.gui.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class MenuText {

    private static final String LANG_PREFIX = "lang:";

    private MenuText() {
    }

    public static String render(String raw, Function<String, String> languageResolver, Map<String, String> placeholders) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        String text = raw;
        if (text.startsWith(LANG_PREFIX)) {
            text = languageResolver.apply(text.substring(LANG_PREFIX.length()));
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return text;
    }

    public static List<String> renderLore(
            List<String> rawLore,
            Function<String, String> languageResolver,
            Map<String, String> placeholders,
            Map<String, List<String>> listPlaceholders
    ) {
        List<String> rendered = new ArrayList<>();
        for (String rawLine : rawLore) {
            String trimmed = rawLine == null ? "" : rawLine.trim();
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                String key = trimmed.substring(1, trimmed.length() - 1);
                List<String> expansion = listPlaceholders.get(key);
                if (expansion != null) {
                    rendered.addAll(expansion);
                    continue;
                }
            }
            rendered.add(render(rawLine, languageResolver, placeholders));
        }
        return List.copyOf(rendered);
    }
}
