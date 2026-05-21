package gg.fotia.enchantment.gui.menu;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuConfigTest {

    @Test
    void parsesSlotRangesAndCommaSeparatedSlots() {
        List<Integer> slots = MenuSlots.parse(List.of("9-11", "13,15", 20));

        assertEquals(List.of(9, 10, 11, 13, 15, 20), slots);
    }

    @Test
    void ignoresInvalidSlotTokens() {
        List<Integer> slots = MenuSlots.parse(List.of("9-bad", "12", "x"));

        assertEquals(List.of(12), slots);
    }

    @Test
    void loadsAdminItemAppearanceFieldsAndAliases() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("menus.admin.rows", 5);
        yaml.set("menus.admin.layout.enchantments", List.of("10-12", 14));
        yaml.set("menus.admin.items.enchantment.material", "knowledge_book");
        yaml.set("menus.admin.items.enchantment.modeldata", 12001);
        yaml.set("menus.admin.items.enchantment.tooltip-style", "minecraft:enchanted_book");
        yaml.set("menus.admin.items.enchantment.itemmodel", "fotiaenchantment:admin_enchantment");
        yaml.set("menus.admin.items.enchantment.glow", true);
        yaml.set("menus.admin.items.enchantment.name", "<!i>{rarity_color}{enchant_name}");
        yaml.set("menus.admin.items.enchantment.lore", List.of("{status}", "{triggers}"));

        MenuConfig config = MenuConfig.from(yaml, "admin");
        MenuItemConfig item = config.item("enchantment", Material.ENCHANTED_BOOK);

        assertEquals(5, config.rows());
        assertEquals(List.of(10, 11, 12, 14), config.enchantmentSlots());
        assertEquals(Material.KNOWLEDGE_BOOK, item.material());
        assertEquals(Integer.valueOf(12001), item.modelData());
        assertEquals("minecraft:enchanted_book", item.tooltipStyle());
        assertEquals("fotiaenchantment:admin_enchantment", item.itemModel());
        assertTrue(item.glow());
        assertEquals("<!i>{rarity_color}{enchant_name}", item.name());
        assertEquals(List.of("{status}", "{triggers}"), item.lore());
    }

    @Test
    void acceptsSingleStringForEnchantmentSlotLayout() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("menus.admin.layout.enchantments", "18-20");

        MenuConfig config = MenuConfig.from(yaml, "admin");

        assertEquals(List.of(18, 19, 20), config.enchantmentSlots());
    }

    @Test
    void loadsDirectGuiFileWithCharacterLayoutRoles() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("title", "lang:admin-gui.title");
        yaml.set("layout", List.of(
                "#########",
                "#AEEEENX#",
                "####P####"
        ));
        yaml.set("roles.background", "#");
        yaml.set("roles.category-all", "A");
        yaml.set("roles.enchantments", "E");
        yaml.set("roles.next-page", "N");
        yaml.set("roles.close", "X");
        yaml.set("roles.previous-page", "P");
        yaml.set("items.enchantment.material", "enchanted_book");

        MenuConfig config = MenuConfig.from(yaml, "admin");

        assertEquals(3, config.rows());
        assertEquals(10, config.roleSlot("category-all", -1));
        assertEquals(List.of(11, 12, 13, 14), config.enchantmentSlots());
        assertEquals(15, config.roleSlot("next-page", -1));
        assertEquals(16, config.roleSlot("close", -1));
        assertEquals(22, config.roleSlot("previous-page", -1));
    }

    @Test
    void roleSlotsFallBackToLegacySlotConfig() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("layout.controls.close", 49);

        MenuConfig config = MenuConfig.from(yaml, "codex");

        assertEquals(49, config.roleSlot("close", 13));
    }

    @Test
    void bundledEnchantmentGuideMenuDefinesGuideSlotsAndAppearance() throws Exception {
        InputStream input = getClass().getClassLoader().getResourceAsStream("gui/enchantment-guide.yml");
        assertNotNull(input);

        YamlConfiguration yaml = new YamlConfiguration();
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            yaml.load(reader);
        }

        MenuConfig config = MenuConfig.from(yaml, "enchantment-guide");
        MenuItemConfig item = config.item("enchantment", Material.BOOK);

        assertEquals(6, config.rows());
        assertEquals("lang:guide-gui.title", config.title());
        assertEquals(36, config.enchantmentSlots().size());
        assertEquals(1, config.roleSlot("category-all", -1));
        assertEquals(Material.ENCHANTED_BOOK, item.material());
        assertEquals("lang:guide-gui.enchantment-name", item.name());
        assertTrue(item.glow());
        assertTrue(item.lore().contains("{effect_lines}"));
        assertFalse(item.lore().contains("{description_lines}"));
    }

    @Test
    void bundledGuideDetailLineShowsEffectOnlyAndKeepsChancePlaceholder() throws Exception {
        for (String locale : List.of("zh_cn", "zh_tw", "en_us", "ja_jp", "ko_kr")) {
            InputStream input = getClass().getClassLoader().getResourceAsStream("lang/" + locale + "/gui.yml");
            assertNotNull(input, locale);

            YamlConfiguration yaml = new YamlConfiguration();
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                yaml.load(reader);
            }

            String detailLine = yaml.getString("guide-gui.detail-line", "");
            String chanceLine = yaml.getString("guide-gui.detail-chance", "");
            String potionLine = yaml.getString("guide-gui.effect-phrase-ADD_POTION_SELF", "");
            String speedLine = yaml.getString("guide-gui.effect-phrase-SPEED_BOOST", "");
            assertFalse(detailLine.contains("{trigger}"), locale);
            assertTrue(detailLine.contains("{chance_phrase}"), locale);
            assertTrue(detailLine.contains("{effects}"), locale);
            assertTrue(chanceLine.contains("{chance}"), locale);
            assertTrue(potionLine.contains("{amplifier}"), locale);
            assertTrue(potionLine.contains("{seconds}"), locale);
            assertTrue(speedLine.contains("{amplifier}"), locale);
            assertTrue(speedLine.contains("{seconds}"), locale);
        }
    }

    @Test
    void bundledAdminMenuDefinesVisibleShortcutHelp() throws Exception {
        InputStream input = getClass().getClassLoader().getResourceAsStream("gui/admin.yml");
        assertNotNull(input);

        YamlConfiguration yaml = new YamlConfiguration();
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            yaml.load(reader);
        }

        MenuConfig config = MenuConfig.from(yaml, "admin");
        MenuItemConfig shortcutHelp = config.item("shortcut-help", Material.KNOWLEDGE_BOOK);

        assertEquals(51, config.roleSlot("shortcut-help", -1));
        assertEquals(Material.KNOWLEDGE_BOOK, shortcutHelp.material());
        assertEquals("lang:admin-gui.shortcut-help", shortcutHelp.name());
        assertTrue(shortcutHelp.lore().contains("lang:admin-gui.shortcut-help-give-max"));
        assertTrue(shortcutHelp.lore().contains("lang:admin-gui.shortcut-help-give-one"));
    }

    @Test
    void bundledDisenchantMenuDefinesCustomizableSlotsAndAppearance() throws Exception {
        InputStream input = getClass().getClassLoader().getResourceAsStream("gui/disenchant.yml");
        assertNotNull(input);

        YamlConfiguration yaml = new YamlConfiguration();
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            yaml.load(reader);
        }

        MenuConfig config = MenuConfig.from(yaml, "disenchant");
        MenuItemConfig enchantment = config.item("enchantment", Material.BOOK);
        MenuItemConfig confirm = config.item("confirm", Material.STONE);

        assertEquals(4, config.rows());
        assertEquals("lang:disenchant-gui.title", config.title());
        assertEquals(4, config.roleSlot("equipment", -1));
        assertEquals(7, config.roleSlot("stone", -1));
        assertEquals(29, config.roleSlot("cancel", -1));
        assertEquals(31, config.roleSlot("info", -1));
        assertEquals(33, config.roleSlot("confirm", -1));
        assertEquals(List.of(18, 19, 20, 21, 22, 23, 24, 25, 26), config.enchantmentSlots());
        assertEquals(Material.ENCHANTED_BOOK, enchantment.material());
        assertEquals("lang:disenchant-gui.enchantment-name", enchantment.name());
        assertEquals(Material.LIME_STAINED_GLASS_PANE, confirm.material());
    }
}
