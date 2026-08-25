package gg.fotia.enchantment.core;

import java.util.Locale;

public enum MobDropOverflowAction {
    TRIM,
    REMOVE_DROP;

    public static MobDropOverflowAction parse(String value) {
        if (value == null || value.isBlank()) {
            return TRIM;
        }
        String normalized = value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return TRIM;
        }
    }
}
