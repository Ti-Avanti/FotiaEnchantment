package gg.fotia.enchantment.lore.item;

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

    @Test
    void stripsStaleGeneratedLoreForSameEnchantmentWhenLevelChanged() {
        List<Component> generated = List.of(
                Component.text("韧性 V"),
                Component.text("    - 5级：有 18% 概率恢复 40% 生命并获得抗性提升 2 级，持续 8 秒。")
        );
        List<Component> playerLore = List.of(Component.text("player lore"));
        List<Component> existing = List.of(
                Component.text("韧性 I"),
                Component.text("    - 1级：有 10% 概率恢复 10% 生命并获得抗性提升 1 级，持续 2 秒。"),
                Component.empty(),
                playerLore.getFirst()
        );

        assertEquals(playerLore, EnchantmentLoreCleaner.stripGeneratedLoreCopies(existing, generated));
    }

    @Test
    void stripsStaleGeneratedLoreDescriptionsWithoutDashPrefix() {
        List<Component> generated = List.of(
                Component.text("Blaze Armor V"),
                Component.text("  When hit, 27.1% chance to burn attacker."),
                Component.text("  Also 5% chance to gain fire resistance.")
        );
        List<Component> playerLore = List.of(Component.text("player lore"));
        List<Component> existing = List.of(
                Component.text("Blaze Armor IV"),
                Component.text("  When hit, 22.9% chance to burn attacker."),
                Component.text("  Also 4% chance to gain fire resistance."),
                Component.empty(),
                Component.text("Blaze Armor III"),
                Component.text("  When hit, 18.6% chance to burn attacker."),
                Component.text("  Also 3% chance to gain fire resistance."),
                Component.empty(),
                playerLore.getFirst()
        );

        assertEquals(playerLore, EnchantmentLoreCleaner.stripGeneratedLoreCopies(existing, generated));
    }

    @Test
    void stripsStaleGeneratedLoreStoredAsJsonTextLines() {
        List<Component> generated = List.of(
                Component.text("韧性 V"),
                Component.text("    - 5级：有 18% 概率恢复 40% 生命并获得抗性提升 2 级，持续 8 秒。")
        );
        List<Component> existing = List.of(
                Component.text("{\"text\":\"韧性 I\",\"color\":\"gray\",\"italic\":false}"),
                Component.text("{\"text\":\"    - 1级：有 10% 概率恢复 10% 生命并获得抗性提升 1 级，持续 2 秒。\",\"color\":\"gray\",\"italic\":false}")
        );

        assertEquals(List.of(), EnchantmentLoreCleaner.stripGeneratedLoreCopies(existing, generated));
    }

    @Test
    void prependsGeneratedLoreAndKeepsPlayerLore() {
        List<Component> generated = List.of(
                Component.text("Blazing Blade II"),
                Component.text("  New description")
        );
        List<Component> playerLore = List.of(Component.text("player lore"));

        assertEquals(
                List.of(
                        Component.text("Blazing Blade II"),
                        Component.text("  New description"),
                        Component.empty(),
                        Component.text("player lore")
                ),
                EnchantmentLoreCleaner.mergeGeneratedLore(playerLore, generated)
        );
    }
}
