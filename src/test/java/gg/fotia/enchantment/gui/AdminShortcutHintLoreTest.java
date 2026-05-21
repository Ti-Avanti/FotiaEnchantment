package gg.fotia.enchantment.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminShortcutHintLoreTest {

    @Test
    void appendsShortcutHintsWhenOldMenuLoreDoesNotContainThem() {
        List<String> lore = AdminShortcutHintLore.apply(List.of("{toggle_hint}"), true);

        assertEquals(List.of(
                "{toggle_hint}",
                "lang:admin-gui.give-max-hint",
                "lang:admin-gui.give-one-hint"
        ), lore);
    }

    @Test
    void doesNotDuplicateConfiguredShortcutHints() {
        List<String> lore = AdminShortcutHintLore.apply(List.of(
                "{toggle_hint}",
                "lang:admin-gui.give-max-hint",
                "lang:admin-gui.give-one-hint"
        ), true);

        assertEquals(List.of(
                "{toggle_hint}",
                "lang:admin-gui.give-max-hint",
                "lang:admin-gui.give-one-hint"
        ), lore);
    }

    @Test
    void leavesLoreUntouchedWhenDisabled() {
        List<String> lore = AdminShortcutHintLore.apply(List.of("{toggle_hint}"), false);

        assertEquals(List.of("{toggle_hint}"), lore);
    }
}
