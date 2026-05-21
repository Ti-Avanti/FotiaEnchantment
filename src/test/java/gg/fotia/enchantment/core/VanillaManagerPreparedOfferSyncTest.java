package gg.fotia.enchantment.core;

import org.junit.jupiter.api.Test;

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
}
