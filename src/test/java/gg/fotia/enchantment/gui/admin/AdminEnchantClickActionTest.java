package gg.fotia.enchantment.gui.admin;

import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminEnchantClickActionTest {

    @Test
    void leftClicksToggleEnchantState() {
        assertEquals(AdminEnchantClickAction.Action.TOGGLE, AdminEnchantClickAction.from(ClickType.LEFT));
        assertEquals(AdminEnchantClickAction.Action.TOGGLE, AdminEnchantClickAction.from(ClickType.SHIFT_LEFT));
    }

    @Test
    void rightClickGivesMaxLevelBook() {
        assertEquals(AdminEnchantClickAction.Action.GIVE_MAX_LEVEL_BOOK, AdminEnchantClickAction.from(ClickType.RIGHT));
    }

    @Test
    void shiftRightClickGivesLevelOneBook() {
        assertEquals(AdminEnchantClickAction.Action.GIVE_LEVEL_ONE_BOOK, AdminEnchantClickAction.from(ClickType.SHIFT_RIGHT));
    }

    @Test
    void unrelatedClicksDoNothing() {
        assertEquals(AdminEnchantClickAction.Action.NONE, AdminEnchantClickAction.from(ClickType.DROP));
        assertEquals(AdminEnchantClickAction.Action.NONE, AdminEnchantClickAction.from(null));
    }
}
