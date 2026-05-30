package gg.fotia.enchantment.listener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentDisplayListenerTest {

    @Test
    void normalizesEnchantingTableTopInventoryAfterEnchanting() {
        assertTrue(EnchantmentDisplayListener.shouldNormalizeMechanicTopInventory("ENCHANTING"));
    }

    @Test
    void doesNotNormalizeGenericChestTopInventoryAsMechanicResult() {
        assertFalse(EnchantmentDisplayListener.shouldNormalizeMechanicTopInventory("CHEST"));
    }
}
