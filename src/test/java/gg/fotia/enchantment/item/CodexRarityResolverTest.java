package gg.fotia.enchantment.item;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CodexRarityResolverTest {

    @Test
    void legacyCommonUsesCurrentDustlightPoolWhenRawPoolIsMissing() {
        String rarity = CodexRarityResolver.resolve(
                "common",
                Set.of("dustlight")::contains,
                Set.of("common")::contains
        );

        assertEquals("dustlight", rarity);
    }

    @Test
    void customCommonPoolIsPreservedWhenConfigured() {
        String rarity = CodexRarityResolver.resolve(
                "common",
                Set.of("common", "dustlight")::contains,
                Set.of("common")::contains
        );

        assertEquals("common", rarity);
    }

    @Test
    void configuredUnknownRarityIsPreservedWhenNoAliasPoolExists() {
        String rarity = CodexRarityResolver.resolve(
                "seasonal",
                Set.of("dustlight")::contains,
                Set.of("seasonal")::contains
        );

        assertEquals("seasonal", rarity);
    }

    @Test
    void blankRarityIsRejected() {
        assertNull(CodexRarityResolver.resolve(" ", Set.of("dustlight")::contains, Set.of()::contains));
    }
}
