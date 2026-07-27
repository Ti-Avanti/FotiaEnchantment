package gg.fotia.enchantment.lore.item;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EnchantmentRarityOrder {

    /** 未在 rarity.yml 中定义的稀有度的名次 */
    public static final int UNKNOWN_RANK = Integer.MAX_VALUE - 1;

    private EnchantmentRarityOrder() {
    }

    /**
     * 预计算 稀有度 → 排序名次 表, 供 ConfigManager 在配置加载时缓存。
     * 名次按 weight 升序 (weight 越小越靠前), 相同 weight 保持配置文件顺序。
     */
    public static Map<String, Integer> rankMap(YamlConfiguration rarityConfig) {
        if (rarityConfig == null) {
            return Map.of();
        }
        List<String> keys = new ArrayList<>(rarityConfig.getKeys(false));
        if (keys.isEmpty()) {
            return Map.of();
        }
        List<String> originalOrder = List.copyOf(keys);
        keys.sort(Comparator
                .comparingDouble((String key) -> weight(rarityConfig, key))
                .thenComparingInt(originalOrder::indexOf));

        Map<String, Integer> ranks = new HashMap<>();
        for (int index = 0; index < keys.size(); index++) {
            ranks.put(keys.get(index).toLowerCase(Locale.ROOT), index);
        }
        return Map.copyOf(ranks);
    }

    /**
     * 基于预计算表的 O(1) 名次查询
     */
    public static int rank(Map<String, Integer> ranks, String rarity) {
        if (ranks == null || rarity == null || rarity.isBlank()) {
            return UNKNOWN_RANK;
        }
        Integer rank = ranks.get(rarity.toLowerCase(Locale.ROOT));
        return rank != null ? rank : UNKNOWN_RANK;
    }

    /**
     * 一次性查询 (每次调用重建全表, 仅限低频路径; 热路径请走 ConfigManager.getRarityRank)
     */
    public static int rank(YamlConfiguration rarityConfig, String rarity) {
        return rank(rankMap(rarityConfig), rarity);
    }

    private static double weight(YamlConfiguration rarityConfig, String key) {
        String path = key + ".weight";
        return rarityConfig.isSet(path) ? rarityConfig.getDouble(path) : Double.MAX_VALUE;
    }
}
