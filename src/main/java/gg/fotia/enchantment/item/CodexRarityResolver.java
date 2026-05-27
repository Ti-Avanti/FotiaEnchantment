package gg.fotia.enchantment.item;

import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

public final class CodexRarityResolver {

    private static final Map<String, String> LEGACY_ALIASES = Map.of(
            "normal", "dustlight",
            "common", "dustlight",
            "uncommon", "moonlit",
            "rare", "radiant",
            "epic", "aureate",
            "legendary", "divine",
            "mythic", "divine",
            "mythical", "divine"
    );

    private CodexRarityResolver() {
    }

    public static String resolve(
            String rawRarity,
            Predicate<String> hasCodexPool,
            Predicate<String> isConfiguredRarity
    ) {
        if (rawRarity == null) {
            return null;
        }
        String normalized = rawRarity.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        if (hasCodexPool.test(normalized)) {
            return normalized;
        }

        String alias = LEGACY_ALIASES.get(normalized);
        if (alias != null && hasCodexPool.test(alias)) {
            return alias;
        }
        if (isConfiguredRarity.test(normalized)) {
            return normalized;
        }
        return alias != null ? alias : normalized;
    }

    public static boolean isAliasResult(String rawRarity, String resolvedRarity) {
        if (rawRarity == null || resolvedRarity == null) {
            return false;
        }
        String normalized = rawRarity.trim().toLowerCase(Locale.ROOT);
        return !normalized.equals(resolvedRarity);
    }
}
