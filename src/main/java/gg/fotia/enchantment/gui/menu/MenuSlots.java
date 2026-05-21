package gg.fotia.enchantment.gui.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MenuSlots {

    private MenuSlots() {
    }

    public static List<Integer> parse(Object raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        List<Integer> slots = new ArrayList<>();
        if (raw instanceof Iterable<?> iterable) {
            for (Object entry : iterable) {
                parseEntry(entry, slots);
            }
        } else {
            parseEntry(raw, slots);
        }
        return List.copyOf(slots);
    }

    private static void parseEntry(Object raw, List<Integer> slots) {
        if (raw == null) {
            return;
        }
        if (raw instanceof Number number) {
            slots.add(number.intValue());
            return;
        }

        String text = raw.toString().trim();
        if (text.isEmpty()) {
            return;
        }
        for (String part : text.split(",")) {
            parsePart(part.trim(), slots);
        }
    }

    private static void parsePart(String text, List<Integer> slots) {
        if (text.isEmpty()) {
            return;
        }
        int dash = text.indexOf('-');
        if (dash > 0) {
            Integer start = parseInt(text.substring(0, dash).trim());
            Integer end = parseInt(text.substring(dash + 1).trim());
            if (start == null || end == null) {
                return;
            }
            if (start <= end) {
                for (int slot = start; slot <= end; slot++) {
                    slots.add(slot);
                }
            } else {
                for (int slot = start; slot >= end; slot--) {
                    slots.add(slot);
                }
            }
            return;
        }
        Integer slot = parseInt(text);
        if (slot != null) {
            slots.add(slot);
        }
    }

    private static Integer parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
