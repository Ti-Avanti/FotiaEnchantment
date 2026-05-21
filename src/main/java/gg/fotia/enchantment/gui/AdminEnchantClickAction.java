package gg.fotia.enchantment.gui;

import org.bukkit.event.inventory.ClickType;

final class AdminEnchantClickAction {

    enum Action {
        TOGGLE,
        GIVE_LEVEL_ONE_BOOK,
        GIVE_MAX_LEVEL_BOOK,
        NONE
    }

    private AdminEnchantClickAction() {
    }

    static Action from(ClickType clickType) {
        if (clickType == null) {
            return Action.NONE;
        }
        return switch (clickType) {
            case LEFT, SHIFT_LEFT -> Action.TOGGLE;
            case RIGHT -> Action.GIVE_MAX_LEVEL_BOOK;
            case SHIFT_RIGHT -> Action.GIVE_LEVEL_ONE_BOOK;
            default -> Action.NONE;
        };
    }
}
