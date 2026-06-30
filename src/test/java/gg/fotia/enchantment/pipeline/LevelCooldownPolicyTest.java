package gg.fotia.enchantment.pipeline;

import gg.fotia.enchantment.core.EnchantmentData;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LevelCooldownPolicyTest {

    @Test
    void fixedCooldownRemainsBackwardCompatible() {
        EnchantmentData.EffectBlock block = new EnchantmentData.EffectBlock();
        block.setCooldown(100);

        long ticks = LevelCooldownPolicy.resolveCooldownTicks(
                block,
                3,
                Map.of("level", 3.0)
        );

        assertEquals(100L, ticks);
    }

    @Test
    void levelCooldownOverridesFormulaAndFixedCooldownForMatchingLevel() {
        EnchantmentData.EffectBlock block = new EnchantmentData.EffectBlock();
        block.setCooldown(100);
        block.setCooldownFormula("180 - level * 30");
        block.setCooldownLevels(Map.of(3, 45));

        long ticks = LevelCooldownPolicy.resolveCooldownTicks(
                block,
                3,
                Map.of("level", 3.0)
        );

        assertEquals(45L, ticks);
    }

    @Test
    void formulaCooldownUsesLevelWhenNoExactLevelEntryExists() {
        EnchantmentData.EffectBlock block = new EnchantmentData.EffectBlock();
        block.setCooldown(100);
        block.setCooldownFormula("180 - level * 30");
        block.setCooldownLevels(Map.of(1, 160));

        long ticks = LevelCooldownPolicy.resolveCooldownTicks(
                block,
                2,
                Map.of("level", 2.0)
        );

        assertEquals(120L, ticks);
    }

    @Test
    void invalidFormulaFallsBackToFixedCooldown() {
        EnchantmentData.EffectBlock block = new EnchantmentData.EffectBlock();
        block.setCooldown(90);
        block.setCooldownFormula("level **");

        long ticks = LevelCooldownPolicy.resolveCooldownTicks(
                block,
                2,
                Map.of("level", 2.0)
        );

        assertEquals(90L, ticks);
    }
}
