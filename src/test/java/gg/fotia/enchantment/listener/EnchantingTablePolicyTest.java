package gg.fotia.enchantment.listener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnchantingTablePolicyTest {

    @Test
    void lowCostStillGetsAtLeastOneCustomRoll() {
        assertEquals(1, EnchantingTablePolicy.customRollAttempts(1, 3));
    }

    @Test
    void highCostCanRollMultipleCustomEnchantments() {
        assertEquals(3, EnchantingTablePolicy.customRollAttempts(30, 3));
    }

    @Test
    void customRollChanceIsCapped() {
        assertEquals(0.95, EnchantingTablePolicy.customRollChance(30, 0.35, 0.03, 0.95));
    }
}
