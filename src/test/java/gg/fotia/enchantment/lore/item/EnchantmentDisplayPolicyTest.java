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

    @Test
    void displaysEmptySlotLoreOnlyForEligibleNonStackableItems() {
        assertTrue(EnchantmentDisplayPolicy.shouldDisplayEnchantSlotLore(0, true, 1));
        assertFalse(EnchantmentDisplayPolicy.shouldDisplayEnchantSlotLore(0, true, 64));
        assertFalse(EnchantmentDisplayPolicy.shouldDisplayEnchantSlotLore(0, false, 1));
    }

    @Test
    void keepsSlotLoreForItemsThatAlreadyHaveEnchantments() {
        assertTrue(EnchantmentDisplayPolicy.shouldDisplayEnchantSlotLore(1, true, 64));
    }

    @Test
    void doesNotDisplaySlotLoreForIneligibleItemsEvenWhenUnsafeEnchanted() {
        assertFalse(EnchantmentDisplayPolicy.shouldDisplayEnchantSlotLore(1, false, 1));
    }

    @Test
    void packetDecorationDoesNotInjectSlotOnlyLoreIntoMenuItems() {
        assertFalse(EnchantmentDisplayPolicy.shouldDecoratePacketOnlyEnchantSlotLore(0));
        assertTrue(EnchantmentDisplayPolicy.shouldDecoratePacketOnlyEnchantSlotLore(1));
    }
}
