package gg.fotia.enchantment.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DisenchantTargetTest {

    @Test
    void selectionKeyPreservesSourceAndId() {
        DisenchantTarget fotia = new DisenchantTarget(DisenchantTargetType.FOTIA, "life_steal", 3);
        DisenchantTarget vanilla = new DisenchantTarget(DisenchantTargetType.VANILLA, "minecraft:sharpness", 5);

        assertEquals("fotia:life_steal", fotia.selectionKey());
        assertEquals("vanilla:minecraft:sharpness", vanilla.selectionKey());
        assertEquals(fotia, DisenchantTarget.fromSelectionKey("fotia:life_steal", 3));
        assertEquals(vanilla, DisenchantTarget.fromSelectionKey("vanilla:minecraft:sharpness", 5));
    }

    @Test
    void plainLegacySelectionKeyStillMeansFotia() {
        assertEquals(new DisenchantTarget(DisenchantTargetType.FOTIA, "life_steal", 2),
                DisenchantTarget.fromSelectionKey("life_steal", 2));
    }

    @Test
    void invalidSelectionKeyIsRejected() {
        assertNull(DisenchantTarget.fromSelectionKey("", 1));
        assertNull(DisenchantTarget.fromSelectionKey("vanilla:", 1));
        assertNull(DisenchantTarget.fromSelectionKey("unknown:sharpness", 1));
    }
}
