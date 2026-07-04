package gg.fotia.enchantment.pipeline.effect.impl;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BonusDropEffectExclusionTest {

    @Test
    void excludedBlocksMatchExactMaterialsAndMinecraftKeys() {
        Map<String, Object> params = Map.of(
                "excluded-blocks", List.of("ANCIENT_DEBRIS", "minecraft:chest")
        );

        assertTrue(BonusDropEffect.isExcludedBlock(Material.ANCIENT_DEBRIS, params));
        assertTrue(BonusDropEffect.isExcludedBlock(Material.CHEST, params));
        assertFalse(BonusDropEffect.isExcludedBlock(Material.DIAMOND_ORE, params));
    }

    @Test
    void excludedBlockGroupsMatchAllShulkerBoxes() {
        Map<String, Object> params = Map.of(
                "excluded-block-groups", List.of("SHULKER_BOX")
        );

        assertTrue(BonusDropEffect.isExcludedBlock(Material.SHULKER_BOX, params));
        assertTrue(BonusDropEffect.isExcludedBlock(Material.WHITE_SHULKER_BOX, params));
        assertTrue(BonusDropEffect.isExcludedBlock(Material.BLACK_SHULKER_BOX, params));
        assertFalse(BonusDropEffect.isExcludedBlock(Material.ENDER_CHEST, params));
    }
}
