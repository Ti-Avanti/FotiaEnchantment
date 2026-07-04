package gg.fotia.enchantment.listener;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantingTablePolicyTest {

    @Test
    void lowCostStillGetsAtLeastOneCustomRoll() {
        assertEquals(1, EnchantingTablePolicy.customRollAttempts(1, 3));
    }

    @Test
    void highCostCanRollMultipleCustomEnchantments() {
        assertEquals(3, EnchantingTablePolicy.customRollAttempts(30, 3));
    }

    @Test
    void customRollChanceIsCapped() {
        assertEquals(0.95, EnchantingTablePolicy.customRollChance(30, 0.35, 0.03, 0.95));
    }

    @Test
    void customEnchantingTableRollsSkipItemsWithExistingFotiaEnchantments() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/listener/EnchantListener.java"));

        assertTrue(source.contains("!pdc.getEnchantments(item).isEmpty()"),
                "Custom enchanting table rolls must not run on items that already have Fotia enchantments");
    }

    @Test
    void customEnchantingTableRollsTreatPlainBooksAsEnchantmentCarriers() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/gg/fotia/enchantment/listener/EnchantListener.java"));

        assertTrue(source.contains("isEnchantingTableBook(item)"),
                "Custom enchanting table rolls must identify plain books as enchantment carriers");
        assertTrue(source.contains("!isEnchantingTableBook(item) && !pdc.isApplicable(item, data)"),
                "Custom enchanting table rolls must not reject Fotia book candidates because they are not applicable to BOOK");
    }
}
