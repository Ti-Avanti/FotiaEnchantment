package gg.fotia.enchantment.lang;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageManagerFallbackTest {

    @Test
    void resolvesMissingUserGuiKeyFromBundledDefaults() {
        YamlConfiguration userConfig = new YamlConfiguration();
        userConfig.set("admin-gui.title", "<!i><gold>Admin");

        YamlConfiguration bundledConfig = new YamlConfiguration();
        bundledConfig.set("admin-gui.max-level", "<!i><gray>Max level: <white>{max_level}");

        String resolved = LanguageManager.resolveString(
                "admin-gui.max-level",
                "admin-gui.max-level",
                userConfig,
                bundledConfig
        );

        assertEquals("<!i><gray>Max level: <white>{max_level}", resolved);
    }

    @Test
    void resolvesMissingLocaleKeyFromDefaultLanguage() {
        YamlConfiguration missingLocaleConfig = new YamlConfiguration();
        YamlConfiguration defaultConfig = new YamlConfiguration();
        defaultConfig.set("reload-success", "{prefix}<!i><green>Configuration reloaded");

        String resolved = LanguageManager.resolveString(
                "reload-success",
                "reload-success",
                missingLocaleConfig,
                defaultConfig
        );

        assertEquals("{prefix}<!i><green>Configuration reloaded", resolved);
    }

    @Test
    void normalizesKnownClientLocaleAliases() {
        assertEquals("zh_tw", LanguageManager.normalizeLocale("zh_hk"));
        assertEquals("zh_tw", LanguageManager.normalizeLocale("zh_mo"));
        assertEquals("en_us", LanguageManager.normalizeLocale("en_gb"));
        assertEquals("ja_jp", LanguageManager.normalizeLocale("ja_jp"));
        assertEquals("ko_kr", LanguageManager.normalizeLocale("ko-kr"));
    }

    @Test
    void refreshesOldGuideDescriptionTemplatesInExistingGuiLanguageFiles() {
        assertTrue(LanguageManager.shouldRefreshBundledLanguageValue(
                "lang/zh_cn/gui.yml",
                "guide-gui.detail-line",
                "<!i><dark_gray>  - <gray>{level}级：{trigger}时{chance_phrase}{effects}。"
        ));
        assertTrue(LanguageManager.shouldRefreshBundledLanguageValue(
                "lang/en_us/gui.yml",
                "guide-gui.effect-phrase-SPEED_BOOST",
                "increase speed by level {amplifier}"
        ));
        assertFalse(LanguageManager.shouldRefreshBundledLanguageValue(
                "lang/en_us/gui.yml",
                "guide-gui.detail-line",
                "<!i><dark_gray>  - <gray>Level {level}: {chance_phrase}{effects}."
        ));
    }

    @Test
    void skipsBundledEnchantmentLanguageWhenExternalEnchantmentsAlreadyExist() {
        assertFalse(LanguageManager.shouldMaterializeBundledLanguageResource(
                "lang/zh_cn/enchantments.yml",
                false
        ));
        assertFalse(LanguageManager.shouldMaterializeBundledLanguageResource(
                "lang/en_us/enchantments.yml",
                false
        ));
        assertTrue(LanguageManager.shouldMaterializeBundledLanguageResource(
                "lang/zh_cn/messages.yml",
                false
        ));
        assertTrue(LanguageManager.shouldMaterializeBundledLanguageResource(
                "lang/zh_cn/enchantments.yml",
                true
        ));
    }

    @Test
    void bundledAdditionalLanguagesProvideRequiredFiles() {
        for (String locale : Set.of("zh_tw", "ja_jp", "ko_kr")) {
            for (String fileName : Set.of("messages", "enchantments", "items", "gui")) {
                String resource = "lang/" + locale + "/" + fileName + ".yml";
                InputStream input = getClass().getClassLoader().getResourceAsStream(resource);
                assertNotNull(input, resource);
            }
        }
    }
}
