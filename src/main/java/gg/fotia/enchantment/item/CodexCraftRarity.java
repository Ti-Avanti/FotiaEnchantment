package gg.fotia.enchantment.item;

import org.bukkit.configuration.file.YamlConfiguration;

public final class CodexCraftRarity {

    public static final String DEFAULT_RARITY = "dustlight";
    public static final String RANDOM = "random";

    private CodexCraftRarity() {
    }

    public static String resolve(String configured, YamlConfiguration rarityConfig) {
        return RANDOM;
    }

    public static boolean isRandom(String rarity) {
        return rarity == null || RANDOM.equalsIgnoreCase(rarity.trim());
    }
}
