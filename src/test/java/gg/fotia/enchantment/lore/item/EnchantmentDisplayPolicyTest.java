package gg.fotia.enchantment.lore.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentDisplayPolicyTest {

    @Test
    void hidesNativeDisplayForAnyManagedEnchantSource() {
        assertTrue(EnchantmentDisplayPolicy.shouldHideNativeEnchantments(true, false, false));
        assertTrue(EnchantmentDisplayPolicy.shouldHideNativeEnchantments(false, true, false));
        assertTrue(EnchantmentDisplayPolicy.shouldHideNativeEnchantments(false, false, true));
    }

    @Test
    void leavesPlainItemsUntouched() {
        assertFalse(EnchantmentDisplayPolicy.shouldHideNativeEnchantments(false, false, false));
    }
}
