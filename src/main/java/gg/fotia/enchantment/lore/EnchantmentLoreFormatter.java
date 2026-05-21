package gg.fotia.enchantment.lore;

public final class EnchantmentLoreFormatter {

    private static final String DEFAULT_CUSTOM_COLOR = "<white>";
    private static final String DEFAULT_VANILLA_COLOR = "<aqua>";
    private static final String CURSE_COLOR = "<red>";

    private EnchantmentLoreFormatter() {
    }

    public static String customDisplayLine(String name, int level, String rarityColor, boolean curse) {
        return "<!i>" + (curse ? CURSE_COLOR : normalizeColor(rarityColor, DEFAULT_CUSTOM_COLOR))
                + name + " " + toRoman(level);
    }

    public static String vanillaDisplayLine(String name, int level, boolean curse) {
        return "<!i>" + (curse ? CURSE_COLOR : DEFAULT_VANILLA_COLOR)
                + name + " " + toRoman(level);
    }

    public static String descriptionLine(String description) {
        return "<!i><dark_gray>  " + description;
    }

    public static String toRoman(int num) {
        if (num <= 0) {
            return String.valueOf(num);
        }
        String[] ones = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (num <= 10) {
            return ones[num];
        }
        return String.valueOf(num);
    }

    private static String normalizeColor(String color, String fallback) {
        if (color == null || color.isBlank()) {
            return fallback;
        }
        return color;
    }
}
