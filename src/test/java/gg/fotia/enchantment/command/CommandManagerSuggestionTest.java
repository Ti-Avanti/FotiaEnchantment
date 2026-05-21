package gg.fotia.enchantment.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandManagerSuggestionTest {

    @Test
    void normalizesSingleRawPaperSuggestionArgumentAndPreservesTrailingSlot() {
        assertArrayEquals(
                new String[]{"give", ""},
                CommandManager.normalizeArgsForSuggestions(new String[]{"give "}));
        assertArrayEquals(
                new String[]{"give", "blazing_blade", ""},
                CommandManager.normalizeArgsForSuggestions(new String[]{"give blazing_blade "}));
    }

    @Test
    void leavesAlreadySplitArgumentsUntouched() {
        assertArrayEquals(
                new String[]{"give", ""},
                CommandManager.normalizeArgsForSuggestions(new String[]{"give", ""}));
        assertArrayEquals(
                new String[]{""},
                CommandManager.normalizeArgsForSuggestions(new String[]{""}));
    }

    @Test
    void guiCommandDefaultsToPlayerAccessibleGuideEntry() {
        var command = new gg.fotia.enchantment.command.impl.GUICommand(null);

        assertEquals("fotia.enchantment.use", command.getPermission());
        assertEquals(java.util.List.of("guide"), command.tabComplete(null, new String[]{""}));
    }
}
