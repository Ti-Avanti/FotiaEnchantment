package gg.fotia.enchantment.lore.item;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class EnchantmentRarityOrder {

    private static final int UNKNOWN_RANK = Integer.MAX_VALUE - 1;

    private EnchantmentRarityOrder() {
    }

    public static int rank(YamlConfiguration rarityConfig, String rarity) {
        if (rarityConfig == null || rarity == null || rarity.isBlank()) {
            return UNKNOWN_RANK;
        }

        String normalized = rarity.toLowerCase(Locale.ROOT);
        List<String> keys = new ArrayList<>(rarityConfig.getKeys(false));
        List<String> originalOrder = List.copyOf(keys);
        keys.sort(Comparator
                .comparingDouble((String key) -> weight(rarityConfig, key))
                .thenComparingInt(originalOrder::indexOf));

        for (int index = 0; index < keys.size(); index++) {
            if (keys.get(index).equalsIgnoreCase(normalized)) {
                return index;
            }
        }
        return UNKNOWN_RANK;
    }

    private static double weight(YamlConfiguration rarityConfig, String key) {
        String path = key + ".weight";
        return rarityConfig.isSet(path) ? rarityConfig.getDouble(path) : Double.MAX_VALUE;
    }
}
