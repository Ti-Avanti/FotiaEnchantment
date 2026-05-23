package gg.fotia.enchantment.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentConfigPersistenceTest {

    @TempDir
    Path tempDir;

    @Test
    void enabledFlagCanBePersistedToEnchantmentFile() throws IOException {
        Path file = tempDir.resolve("resilience.yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("enabled", true);
        config.set("max-level", 5);
        config.save(file.toFile());

        assertTrue(EnchantmentConfig.saveEnabledFlag(file.toFile(), false));

        YamlConfiguration reloaded = YamlConfiguration.loadConfiguration(file.toFile());
        assertFalse(reloaded.getBoolean("enabled", true));
    }
}
