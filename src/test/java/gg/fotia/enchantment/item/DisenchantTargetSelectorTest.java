package gg.fotia.enchantment.item;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisenchantTargetSelectorTest {

    @Test
    void selectableModeDoesNotFallBackToRandomWhenNothingSelected() {
        List<DisenchantTarget> available = List.of(
                new DisenchantTarget(DisenchantTargetType.FOTIA, "frost_shield", 4)
        );

        List<DisenchantTarget> selected = DisenchantTargetSelector.select(
                available,
                true,
                List.of(),
                1,
                false
        );

        assertTrue(selected.isEmpty());
    }

    @Test
    void selectableModeIgnoresInvalidSelectionKeys() {
        List<DisenchantTarget> available = List.of(
                new DisenchantTarget(DisenchantTargetType.FOTIA, "frost_shield", 4)
        );

        List<DisenchantTarget> selected = DisenchantTargetSelector.select(
                available,
                true,
                List.of("fotia:missing"),
                1,
                false
        );

        assertTrue(selected.isEmpty());
    }

    @Test
    void randomModeCanUseAvailableTargetsWithoutSelection() {
        DisenchantTarget target = new DisenchantTarget(DisenchantTargetType.FOTIA, "frost_shield", 4);

        List<DisenchantTarget> selected = DisenchantTargetSelector.select(
                List.of(target),
                false,
                List.of(),
                1,
                false
        );

        assertEquals(List.of(target), selected);
    }
}
