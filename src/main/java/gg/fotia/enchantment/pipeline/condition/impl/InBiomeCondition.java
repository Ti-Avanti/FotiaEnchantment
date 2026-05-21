package gg.fotia.enchantment.pipeline.condition.impl;

import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.pipeline.condition.Condition;
import gg.fotia.enchantment.pipeline.condition.ConditionContext;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 生物群系条件 - 玩家所在群系是否在指定列表中
 */
public class InBiomeCondition implements Condition {

    @Override
    public String getId() {
        return "in_biome";
    }

    @Override
    public boolean check(ConditionContext context) {
        Player player = context.getTriggerContext().getPlayer();
        if (player == null) {
            return false;
        }
        EnchantmentData.ConditionConfig cfg = context.getConfig();
        if (cfg == null) {
            return false;
        }
        List<String> biomes = cfg.getStringList("value");
        if (biomes.isEmpty()) {
            String single = cfg.getString("value");
            if (single == null || single.isEmpty()) {
                return false;
            }
            biomes = List.of(single);
        }

        Biome biome = player.getLocation().getBlock().getBiome();
        NamespacedKey key = biome.getKey();
        String name = key.getKey().toLowerCase();
        String full = key.asString().toLowerCase();
        for (String b : biomes) {
            if (b == null) continue;
            String bl = b.toLowerCase();
            if (bl.equals(name) || bl.equals(full)) {
                return true;
            }
        }
        return false;
    }
}
