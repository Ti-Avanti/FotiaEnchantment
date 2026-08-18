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
    void scheduledNormalizationRemovesDisabledVanillaEnchantments() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/listener/EnchantmentDisplayListener.java"));

        assertTrue(source.contains("scheduleNormalize(player)"),
                "Periodic validity scanning must schedule player inventory normalization");
        assertTrue(source.contains("removeDisabledEnchantments(item)"),
                "Scheduled inventory normalization must apply vanilla disabled enchantment config");
    }

    @Test
    void localeChangesRefreshPacketDisplayWithoutNormalizingStoredLore() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/listener/EnchantmentDisplayListener.java"));

        assertTrue(source.contains("PlayerLocaleChangeEvent"),
                "Client locale changes must trigger a fresh localized packet view");
        assertTrue(source.contains("scheduleClientRefresh(event.getPlayer())"),
                "Locale changes must refresh packets instead of rewriting persistent item lore");
    }
}
