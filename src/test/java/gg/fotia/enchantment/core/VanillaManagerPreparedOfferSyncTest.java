package gg.fotia.enchantment.core;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void spearTokenMatchesSpearMaterialsOnly() {
        Material spear = Material.valueOf("NETHERITE_SPEAR");

        assertTrue(VanillaManager.matchesApplicableItemToken(spear, "SPEAR"));
        assertFalse(VanillaManager.matchesApplicableItemToken(spear, "SWORD"));
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

    @Test
    void anvilMergeAddsMissingIncomingLevel() {
        assertEquals(5, VanillaManager.mergeAnvilLevel(0, 5, 10));
    }

    @Test
    void anvilMergeUpgradesEqualLevelsAndCapsAtMax() {
        assertEquals(4, VanillaManager.mergeAnvilLevel(3, 3, 10));
        assertEquals(10, VanillaManager.mergeAnvilLevel(10, 10, 10));
    }

    @Test
    void anvilMergeKeepsHigherDifferentLevel() {
        assertEquals(5, VanillaManager.mergeAnvilLevel(3, 5, 10));
        assertEquals(5, VanillaManager.mergeAnvilLevel(5, 3, 10));
    }

    @Test
    void enchantingTableWeightedPreviewRollIsStableForSameContext() {
        int first = VanillaManager.stableEnchantingPreviewRoll(
                64,
                123456789,
                Material.DIAMOND_SWORD,
                2,
                30,
                15,
                "minecraft:sharpness",
                4
        );

        int second = VanillaManager.stableEnchantingPreviewRoll(
                64,
                123456789,
                Material.DIAMOND_SWORD,
                2,
                30,
                15,
                "minecraft:sharpness",
                4
        );

        assertTrue(first >= 0 && first < 64);
        assertTrue(second >= 0 && second < 64);
        assertTrue(first == second);
    }

    @Test
    void enchantingTableWeightedPreviewRollChangesWhenEnchantingSeedChanges() {
        int first = VanillaManager.stableEnchantingPreviewRoll(
                64,
                123456789,
                Material.DIAMOND_SWORD,
                2,
                30,
                15,
                "minecraft:sharpness",
                4
        );
        boolean changed = false;
        for (int seed = 123456790; seed < 123456806; seed++) {
            int next = VanillaManager.stableEnchantingPreviewRoll(
                    64,
                    seed,
                    Material.DIAMOND_SWORD,
                    2,
                    30,
                    15,
                    "minecraft:sharpness",
                    4
            );
            if (next != first) {
                changed = true;
                break;
            }
        }

        assertTrue(changed);
    }
}
