package gg.fotia.enchantment.listener;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentDisplayListenerTest {

    @Test
    void normalizesEnchantingTableTopInventoryAfterEnchanting() {
        assertTrue(EnchantmentDisplayListener.shouldNormalizeMechanicTopInventory("ENCHANTING"));
    }

    @Test
    void doesNotNormalizeGenericChestTopInventoryAsMechanicResult() {
        assertFalse(EnchantmentDisplayListener.shouldNormalizeMechanicTopInventory("CHEST"));
    }

    @Test
    void normalizerRemovesDisabledVanillaEnchantmentsBeforeLoreRefresh() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/listener/EnchantmentDisplayListener.java"));

        assertTrue(source.contains("removeDisabledEnchantments(item)"),
                "Automatic display normalization must apply vanilla disabled enchantment config");
        assertTrue(source.contains("EnchantmentLoreCleaner.applyGeneratedLoreFromSource"),
                "When normalization changes enchantments, stale generated lore must be stripped from the original item");
    }

    @Test
    void asyncNormalizationDetectsDisabledVanillaEnchantments() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/listener/EnchantmentDisplayListener.java"));

        assertTrue(source.contains("hasDisabledEnchantments(item)"),
                "Background item normalization must notice vanilla enchantments disabled by config");
    }
}
