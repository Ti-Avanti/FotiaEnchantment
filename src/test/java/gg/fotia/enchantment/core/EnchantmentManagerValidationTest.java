package gg.fotia.enchantment.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnchantmentManagerValidationTest {

    @Test
    void undefinedConflictKeepsSourceFile() {
        EnchantmentData source = new EnchantmentData();
        source.setId("source");
        source.setConflicts(List.of("missing", "fotiaenchantment:defined", "minecraft:sharpness"));

        EnchantmentData defined = new EnchantmentData();
        defined.setId("defined");

        List<EnchantmentManager.UndefinedConflict> conflicts = EnchantmentManager.findUndefinedConflicts(
                List.of(source, defined),
                id -> "plugins/FotiaEnchantment/enchantments/melee/" + id + ".yml");

        assertEquals(1, conflicts.size());
        assertEquals("source", conflicts.get(0).sourceId());
        assertEquals("missing", conflicts.get(0).conflictId());
        assertEquals("plugins/FotiaEnchantment/enchantments/melee/source.yml", conflicts.get(0).sourceFile());
    }
}
