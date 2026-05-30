package gg.fotia.enchantment.lore.item;

public final class EnchantmentDisplayPolicy {

    private EnchantmentDisplayPolicy() {
    }

    public static boolean shouldHideNativeEnchantments(boolean hasEnchants,
                                                       boolean hasStoredEnchants,
                                                       boolean hasLegacyCustomEnchantments) {
        return hasEnchants || hasStoredEnchants || hasLegacyCustomEnchantments;
    }

    public static boolean shouldDisplayEnchantSlotLore(int usedSlots,
                                                       boolean eligibleForEmptySlots,
                                                       int maxStackSize) {
        if (!eligibleForEmptySlots) {
            return false;
        }
        if (usedSlots > 0) {
            return true;
        }
        return maxStackSize <= 1;
    }

    public static boolean shouldDecoratePacketOnlyEnchantSlotLore(int usedSlots) {
        return usedSlots > 0;
    }
}
