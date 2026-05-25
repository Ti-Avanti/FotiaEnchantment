package gg.fotia.enchantment.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerDefaultEnchantmentsTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultEnchantmentsAreSavedOnlyWhenDirectoryIsMissing() throws IOException {
        assertTrue(ConfigManager.shouldSaveDefaultEnchantments(tempDir.toFile()));

        Files.createDirectories(tempDir.resolve("enchantments").resolve("melee"));

        assertFalse(ConfigManager.shouldSaveDefaultEnchantments(tempDir.toFile()));
    }
}
