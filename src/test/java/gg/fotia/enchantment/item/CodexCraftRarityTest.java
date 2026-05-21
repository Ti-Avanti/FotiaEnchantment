package gg.fotia.enchantment.item;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CodexCraftRarityTest {

    @Test
    void defaultsToRandomCraftRarity() {
        YamlConfiguration rarityConfig = rarityConfig();

        assertTrue(CodexCraftRarity.isRandom(CodexCraftRarity.resolve(null, rarityConfig)));
        assertTrue(CodexCraftRarity.isRandom(CodexCraftRarity.resolve("", rarityConfig)));
    }

    @Test
    void configuredRarityDoesNotForceCraftResult() {
        YamlConfiguration rarityConfig = rarityConfig();

        assertTrue(CodexCraftRarity.isRandom(CodexCraftRarity.resolve("radiant", rarityConfig)));
        assertTrue(CodexCraftRarity.isRandom(CodexCraftRarity.resolve(" AUREATE ", rarityConfig)));
    }

    @Test
    void invalidRarityStillUsesRandomCraftResult() {
        YamlConfiguration rarityConfig = rarityConfig();

        assertTrue(CodexCraftRarity.isRandom(CodexCraftRarity.resolve("missing", rarityConfig)));
    }

    @Test
    void randomModeIsAlwaysEnabledForFragmentCrafting() {
        YamlConfiguration rarityConfig = rarityConfig();

        assertTrue(CodexCraftRarity.isRandom(CodexCraftRarity.resolve("random", rarityConfig)));
    }

    private YamlConfiguration rarityConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("dustlight.weight", 40);
        config.set("radiant.weight", 15);
        config.set("aureate.weight", 8);
        return config;
    }
}
