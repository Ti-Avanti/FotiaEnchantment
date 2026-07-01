package gg.fotia.enchantment.item;

import java.util.Locale;

public enum DisenchantSource {
    FOTIA,
    VANILLA,
    ANY;

    public boolean allows(DisenchantTargetType targetType) {
        if (targetType == null) {
            return false;
        }
        return this == ANY || name().equals(targetType.name());
    }

    public static DisenchantSource fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return FOTIA;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return FOTIA;
        }
    }
}
