package gg.fotia.enchantment.lore.item;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentRarityOrderTest {

    @Test
    void ranksLowerWeightRarityBeforeCommonRarity() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("dustlight.weight", 40);
        config.set("moonlit.weight", 25);
        config.set("divine.weight", 2);

        assertTrue(EnchantmentRarityOrder.rank(config, "divine")
                < EnchantmentRarityOrder.rank(config, "dustlight"));
        assertTrue(EnchantmentRarityOrder.rank(config, "dustlight")
                < EnchantmentRarityOrder.rank(config, "missing"));
    }
}
