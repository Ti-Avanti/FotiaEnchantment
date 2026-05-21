package gg.fotia.enchantment.lore;

public final class EnchantmentDisplayPolicy {

    private EnchantmentDisplayPolicy() {
    }

    public static boolean shouldHideNativeEnchantments(boolean hasEnchants,
                                                       boolean hasStoredEnchants,
                                                       boolean hasLegacyCustomEnchantments) {
        return hasEnchants || hasStoredEnchants || hasLegacyCustomEnchantments;
    }
}
