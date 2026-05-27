package gg.fotia.enchantment.core;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaManagerPreparedOfferSyncTest {

    @Test
    void preparedOfferSyncKeepsMinecraftAndFotiaEnchantments() {
        assertTrue(VanillaManager.shouldKeepNamespaceDuringPreparedOfferSync("minecraft"));
        assertTrue(VanillaManager.shouldKeepNamespaceDuringPreparedOfferSync(EnchantmentRegistry.getNamespace()));
    }

    @Test
    void preparedOfferSyncDropsUnknownNamespaces() {
        assertFalse(VanillaManager.shouldKeepNamespaceDuringPreparedOfferSync("other_plugin"));
        assertFalse(VanillaManager.shouldKeepNamespaceDuringPreparedOfferSync(null));
    }

    @Test
    void vanillaApplicableItemsDoNotUseSubstringMatching() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/core/VanillaManager.java"));

        assertFalse(source.contains("itemType.contains(applicable.toUpperCase(Locale.ROOT))"),
                "AXE must not match DIAMOND_PICKAXE by substring");
    }

    @Test
    void axeTokenDoesNotMatchPickaxeMaterials() {
        assertTrue(VanillaManager.matchesApplicableItemToken(Material.DIAMOND_AXE, "AXE"));
        assertFalse(VanillaManager.matchesApplicableItemToken(Material.DIAMOND_PICKAXE, "AXE"));
    }

    @Test
    void bowTokenDoesNotMatchCrossbowMaterial() {
        assertTrue(VanillaManager.matchesApplicableItemToken(Material.BOW, "BOW"));
        assertFalse(VanillaManager.matchesApplicableItemToken(Material.CROSSBOW, "BOW"));
        assertTrue(VanillaManager.matchesApplicableItemToken(Material.CROSSBOW, "CROSSBOW"));
    }

    @Test
    void exactMaterialTokensStillMatch() {
        assertTrue(VanillaManager.matchesApplicableItemToken(Material.DIAMOND_PICKAXE, "DIAMOND_PICKAXE"));
        assertFalse(VanillaManager.matchesApplicableItemToken(Material.DIAMOND_AXE, "DIAMOND_PICKAXE"));
    }
}
