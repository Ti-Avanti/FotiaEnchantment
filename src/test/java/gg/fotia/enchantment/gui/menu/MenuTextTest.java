package gg.fotia.enchantment.gui.menu;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MenuTextTest {

    @Test
    void resolvesLanguageKeysBeforeReplacingPlaceholders() {
        String text = MenuText.render(
                "lang:admin-gui.max-level",
                key -> "<!i><gray>等级上限: <white>{max_level}",
                Map.of("max_level", "5")
        );

        assertEquals("<!i><gray>等级上限: <white>5", text);
    }

    @Test
    void expandsListPlaceholdersInsideLore() {
        List<String> lore = MenuText.renderLore(
                List.of("{status}", "{triggers}", "lang:admin-gui.click-to-toggle"),
                key -> "<!i><gray>点击切换状态",
                Map.of("status", "<!i><green>● 已启用"),
                Map.of("triggers", List.of("<!i><dark_gray>  - <gray>每 5 秒"))
        );

        assertEquals(List.of(
                "<!i><green>● 已启用",
                "<!i><dark_gray>  - <gray>每 5 秒",
                "<!i><gray>点击切换状态"
        ), lore);
    }
}
