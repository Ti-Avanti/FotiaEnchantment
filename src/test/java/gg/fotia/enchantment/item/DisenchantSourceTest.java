package gg.fotia.enchantment.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisenchantSourceTest {

    @Test
    void sourceControlsWhichEnchantmentsCanBeRemoved() {
        assertTrue(DisenchantSource.FOTIA.allows(DisenchantTargetType.FOTIA));
        assertFalse(DisenchantSource.FOTIA.allows(DisenchantTargetType.VANILLA));

        assertTrue(DisenchantSource.VANILLA.allows(DisenchantTargetType.VANILLA));
        assertFalse(DisenchantSource.VANILLA.allows(DisenchantTargetType.FOTIA));

        assertTrue(DisenchantSource.ANY.allows(DisenchantTargetType.FOTIA));
        assertTrue(DisenchantSource.ANY.allows(DisenchantTargetType.VANILLA));
    }

    @Test
    void invalidOrBlankSourceFallsBackToFotiaForOldConfigs() {
        assertEquals(DisenchantSource.FOTIA, DisenchantSource.fromConfig(null));
        assertEquals(DisenchantSource.FOTIA, DisenchantSource.fromConfig(""));
        assertEquals(DisenchantSource.FOTIA, DisenchantSource.fromConfig("unknown"));
        assertEquals(DisenchantSource.VANILLA, DisenchantSource.fromConfig("vanilla"));
        assertEquals(DisenchantSource.ANY, DisenchantSource.fromConfig("ANY"));
    }
}
