package gg.fotia.enchantment.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PDCManagerGlintTest {

    @Test
    void legacyCustomEnchantmentsForceGlintOverrideWhenWritten() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/core/PDCManager.java"));

        assertTrue(source.contains("setLegacyCustomGlint(meta, true)"),
                "PDC-only Fotia enchantments must explicitly enable the enchantment glint");
        assertTrue(source.contains("ItemUtils.applyPersistentCustomEnchantGlint(meta, true)"),
                "Player items must use the compatibility glint helper instead of fake vanilla enchantments");
    }

    @Test
    void removingLastLegacyCustomEnchantmentClearsGlintOverrideOnlyWhenNoNativeEnchantmentsRemain() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/core/PDCManager.java"));

        assertTrue(source.contains("setLegacyCustomGlint(meta, false)"),
                "Removing the last PDC-only Fotia enchantment must clear the custom glint override");
        assertTrue(source.contains("ItemUtils.applyPersistentCustomEnchantGlint(meta, false)"),
                "PDC-only Fotia enchantment removal must clear the compatibility glint helper state");
        assertTrue(source.contains("!meta.hasEnchants() && !hasStoredEnchantments(meta)"),
                "Glint override must not be cleared while vanilla or true Fotia enchantments remain");
    }
}
