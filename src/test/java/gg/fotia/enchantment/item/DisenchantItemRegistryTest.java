package gg.fotia.enchantment.item;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisenchantItemRegistryTest {

    @Test
    void customDisenchantItemsAreReadFromConfiguredIds() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("disenchant-stone.items.pure-law-glass.source", "VANILLA");
        config.set("disenchant-stone.items.pure-law-glass.success-chance", 70);
        config.set("disenchant-stone.items.star-scar-prism.source", "FOTIA");

        Set<String> itemIds = DisenchantItemRegistry.itemIds(config);
        ConfigurationSection section = DisenchantItemRegistry.section(config, "pure-law-glass");

        assertTrue(itemIds.contains("pure-law-glass"));
        assertTrue(itemIds.contains("star-scar-prism"));
        assertNotNull(section);
        assertEquals(DisenchantSource.VANILLA, DisenchantItemRegistry.source(section));
        assertEquals(70, section.getInt("success-chance"));
        assertEquals("pure-law-glass", DisenchantItemRegistry.configKey(config, "pure-law-glass"));
    }

    @Test
    void legacyDisenchantTiersStillResolveToOldItemIds() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("disenchant-stone.tiers.tier-1.success-chance", 80);
        config.set("disenchant-stone.tiers.tier-2.success-chance", 90);
        config.set("disenchant-stone.tiers.tier-3.success-chance", 100);

        assertEquals("disenchant-shard", DisenchantItemRegistry.itemIdForConfigKey(config, "tier-1"));
        assertEquals("tier-2", DisenchantItemRegistry.configKey(config, "disenchant-crystal"));
        assertNotNull(DisenchantItemRegistry.section(config, "disenchant-gem"));
        assertEquals(DisenchantSource.FOTIA,
                DisenchantItemRegistry.source(DisenchantItemRegistry.section(config, "disenchant-gem")));
    }

    @Test
    void unknownDisenchantItemDoesNotResolve() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("disenchant-stone.items.pure-law-glass.source", "VANILLA");

        assertNull(DisenchantItemRegistry.configKey(config, "missing-stone"));
        assertNull(DisenchantItemRegistry.section(config, "missing-stone"));
    }
}
