package gg.fotia.enchantment.lore.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnchantmentLoreFormatterTest {

    @Test
    void customEnchantUsesRarityColorWhenItIsNotCurse() {
        String line = EnchantmentLoreFormatter.customDisplayLine("Thunder Smash", 3, "<gold>", false);

        assertEquals("<!i><gold>Thunder Smash III", line);
    }

    @Test
    void customCurseUsesVanillaCurseRedInsteadOfRarityColor() {
        String line = EnchantmentLoreFormatter.customDisplayLine("Doom Curse", 2, "<light_purple>", true);

        assertEquals("<!i><red>Doom Curse II", line);
    }

    @Test
    void vanillaCurseUsesRed() {
        String line = EnchantmentLoreFormatter.vanillaDisplayLine("Curse of Binding", 1, true);

        assertEquals("<!i><red>Curse of Binding I", line);
    }
}
