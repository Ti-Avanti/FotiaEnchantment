package gg.fotia.enchantment.item;

import java.util.Locale;

public record DisenchantTarget(DisenchantTargetType type, String id, int level) {

    public DisenchantTarget {
        if (id != null) {
            id = id.trim();
        }
    }

    public String selectionKey() {
        return type.name().toLowerCase(Locale.ROOT) + ":" + id;
    }

    public static DisenchantTarget fromSelectionKey(String selectionKey, int level) {
        if (selectionKey == null || selectionKey.isBlank()) {
            return null;
        }
        String key = selectionKey.trim();
        int colon = key.indexOf(':');
        if (colon < 0) {
            return new DisenchantTarget(DisenchantTargetType.FOTIA, key, level);
        }
        if (colon == key.length() - 1) {
            return null;
        }
        String source = key.substring(0, colon).toUpperCase(Locale.ROOT);
        String id = key.substring(colon + 1);
        try {
            return new DisenchantTarget(DisenchantTargetType.valueOf(source), id, level);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
