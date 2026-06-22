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
    void anvilMergeDoesNotUpgradeWhenResultAlreadyContainsIncomingEnchant() {
        assertEquals(5, VanillaManager.mergeAnvilInputLevel(0, 5, 5, 10));
    }

    @Test
    void anvilMergeUsesNativeAndConfiguredConflictMatrix() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/core/VanillaManager.java"));

        assertTrue(source.contains("conflictsWith("),
                "Vanilla anvil merge must reject Bukkit-native conflicts such as protection variants");
        assertTrue(source.contains("referencesBukkit"),
                "Vanilla anvil merge must still honor configured conflict overrides");
    }

    @Test
    void anvilPrepareRefreshesGeneratedLoreForPreview() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/core/VanillaManager.java"));

        assertTrue(source.contains("EnchantmentLoreCleaner.applyGeneratedLore"),
                "Anvil preview results must refresh generated lore immediately");
    }

    @Test
    void anvilPrepareRefreshesGeneratedLoreFromPreSanitizedResultSource() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/core/VanillaManager.java"));

        assertTrue(source.contains("ItemStack displaySource = result.clone()"),
                "Anvil display cleanup must remember the result before disabled enchantments are removed");
        assertTrue(source.contains("applyAnvilResultDisplay(event, result, displaySource)"),
                "Anvil display cleanup must strip generated lore copied from the pre-sanitized result");
    }

    @Test
    void anvilPrepareBlocksResultsThatAlreadyExceedConfiguredEnchantLimit() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/core/VanillaManager.java"));

        assertTrue(source.contains("isAnvilResultOverLimit"),
                "Vanilla anvil results must be checked against configured enchantment count limits");
        assertTrue(source.contains("event.setResult(null)"),
                "Over-limit anvil results must be blocked instead of previewed or taken");
    }

    @Test
    void grindstonePrepareRefreshesGeneratedLoreFromInputSource() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/core/VanillaManager.java"));

        assertTrue(source.contains("PrepareGrindstoneEvent"),
                "Grindstone preview results must be handled before players take the item");
        assertTrue(source.contains("EnchantmentLoreCleaner.applyGeneratedLoreFromSource"),
                "Grindstone results must strip generated lore copied from the input item");
    }

    @Test
    void grindstonePrepareRemovesCustomEnchantDataBeforeDisplay() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/core/VanillaManager.java"));

        assertTrue(source.contains("removeCustomEnchantments(displayResult)"),
                "Grindstone results must remove Fotia custom enchantment data, not only vanilla enchantments");
        assertTrue(source.contains("pdc.removeEnchantment(item, enchantId)"),
                "Custom enchantment cleanup must remove both legacy PDC and true Fotia enchantments");
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
