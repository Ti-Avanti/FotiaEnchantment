package gg.fotia.enchantment.lore;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnchantmentLoreCleanerTest {

    @Test
    void stripsGeneratedDisplayLoreAndKeepsPlayerLore() {
        List<Component> generated = List.of(
                Component.text("Blazing Blade I"),
                Component.text("  Chance to ignite target")
        );
        List<Component> playerLore = List.of(Component.text("player lore"));
        List<Component> existing = List.of(
                generated.get(0),
                generated.get(1),
                Component.empty(),
                playerLore.get(0)
        );

        assertEquals(playerLore, EnchantmentLoreCleaner.stripGeneratedLoreCopies(existing, generated));
    }
}
