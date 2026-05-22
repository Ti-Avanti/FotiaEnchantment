package gg.fotia.enchantment.gui.admin;

import java.util.ArrayList;
import java.util.List;

final class AdminShortcutHintLore {

    private static final String GIVE_MAX_HINT = "lang:admin-gui.give-max-hint";
    private static final String GIVE_ONE_HINT = "lang:admin-gui.give-one-hint";

    private AdminShortcutHintLore() {
    }

    static List<String> apply(List<String> lore, boolean enabled) {
        if (!enabled) {
            return lore;
        }
        List<String> result = new ArrayList<>(lore);
        if (!result.contains(GIVE_MAX_HINT)) {
            result.add(GIVE_MAX_HINT);
        }
        if (!result.contains(GIVE_ONE_HINT)) {
            result.add(GIVE_ONE_HINT);
        }
        return result;
    }
}
