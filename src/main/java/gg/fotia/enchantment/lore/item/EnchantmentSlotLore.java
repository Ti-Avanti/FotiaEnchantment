package gg.fotia.enchantment.lore.item;

import java.util.Collections;
import java.util.List;

public final class EnchantmentSlotLore {

    public static final String FALLBACK_EMPTY_SLOT = "<!i><dark_gray>[可附魔槽位]";
    public static final String FALLBACK_SLOT_SUMMARY = "<!i><dark_gray>可附魔槽位: {used}/{max}";
    public static final String MODE_LINES = "lines";
    public static final String MODE_SUMMARY = "summary";

    private EnchantmentSlotLore() {
    }

    public static List<String> slotLines(int maxSlots,
                                         int usedSlots,
                                         String displayMode,
                                         String emptySlotLine,
                                         String summaryLine) {
        if (maxSlots < 0) {
            return List.of();
        }
        if (MODE_SUMMARY.equals(normalizeDisplayMode(displayMode))) {
            return List.of(summarySlotLine(maxSlots, usedSlots, summaryLine));
        }
        return emptySlotLines(maxSlots, usedSlots, emptySlotLine);
    }

    public static List<String> emptySlotLines(int maxSlots, int usedSlots, String emptySlotLine) {
        if (maxSlots < 0 || usedSlots >= maxSlots) {
            return List.of();
        }
        String line = emptySlotLine == null || emptySlotLine.isBlank()
                ? FALLBACK_EMPTY_SLOT
                : emptySlotLine;
        return Collections.nCopies(maxSlots - Math.max(0, usedSlots), line);
    }

    public static String normalizeDisplayMode(String displayMode) {
        if (displayMode == null) {
            return MODE_LINES;
        }
        return switch (displayMode.trim().toLowerCase()) {
            case MODE_SUMMARY, "compact", "count", "single-line", "single_line" -> MODE_SUMMARY;
            default -> MODE_LINES;
        };
    }

    private static String summarySlotLine(int maxSlots, int usedSlots, String summaryLine) {
        String line = summaryLine == null || summaryLine.isBlank()
                ? FALLBACK_SLOT_SUMMARY
                : summaryLine;
        return line
                .replace("{used}", String.valueOf(Math.max(0, usedSlots)))
                .replace("{max}", String.valueOf(maxSlots))
                .replace("{remaining}", String.valueOf(Math.max(0, maxSlots - Math.max(0, usedSlots))));
    }
}
