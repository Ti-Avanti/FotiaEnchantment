package gg.fotia.enchantment.lang;

import gg.fotia.enchantment.FotiaEnchantment;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

public class LanguageManager {

    private static final String[] FILE_NAMES = {"messages", "enchantments", "items", "gui"};

    private final FotiaEnchantment plugin;
    private final Map<String, Map<String, YamlConfiguration>> loadedLanguages = new ConcurrentHashMap<>();
    private final Map<String, Map<String, YamlConfiguration>> bundledLanguages = new ConcurrentHashMap<>();
    private String defaultLanguage;

    public LanguageManager(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    public void init() {
        this.defaultLanguage = normalizeLocale(plugin.getConfigManager().getDefaultLanguage());
        ensureBundledLanguageFiles();
        loadLanguage(defaultLanguage);
        loadBundledLanguage(defaultLanguage);
        plugin.getLogger().info("Language system initialized, default language: " + defaultLanguage);
    }

    public String getPlayerLocale(Player player) {
        if (player == null) {
            return defaultLanguage;
        }
        return normalizeLocale(player.locale().toString());
    }

    public String getMessage(Player player, String key) {
        return getMessage(getPlayerLocale(player), key);
    }

    public String getMessage(String locale, String key) {
        return getString(locale, "messages", key, key);
    }

    public String getEnchantName(Player player, String enchantId) {
        String locale = getPlayerLocale(player);
        return getString(locale, "enchantments", enchantId + ".name", enchantId);
    }

    public List<String> getEnchantDescription(Player player, String enchantId) {
        String locale = getPlayerLocale(player);
        return getStringList(locale, "enchantments", enchantId + ".description");
    }

    public String getItemName(Player player, String itemId) {
        String locale = getPlayerLocale(player);
        return getString(locale, "items", itemId + ".name", itemId);
    }

    public List<String> getItemLore(Player player, String itemId) {
        String locale = getPlayerLocale(player);
        return getStringList(locale, "items", itemId + ".lore");
    }

    public String getGUIText(Player player, String key) {
        String locale = getPlayerLocale(player);
        return getString(locale, "gui", key, key);
    }

    public void reload() {
        loadedLanguages.clear();
        bundledLanguages.clear();
        this.defaultLanguage = normalizeLocale(plugin.getConfigManager().getDefaultLanguage());
        ensureBundledLanguageFiles();
        loadLanguage(defaultLanguage);
        loadBundledLanguage(defaultLanguage);
        plugin.getLogger().info("Language system reloaded.");
    }

    private void ensureBundledLanguageFiles() {
        for (String resourcePath : listBundledLanguageResources()) {
            File file = new File(plugin.getDataFolder(), resourcePath);
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                plugin.saveResource(resourcePath, false);
                continue;
            }
            mergeMissingLanguageKeys(resourcePath, file);
        }
    }

    private void mergeMissingLanguageKeys(String resourcePath, File file) {
        try (InputStream input = plugin.getResource(resourcePath)) {
            if (input == null) {
                return;
            }
            YamlConfiguration bundled = new YamlConfiguration();
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                bundled.load(reader);
            }

            YamlConfiguration external = YamlConfiguration.loadConfiguration(file);
            boolean changed = false;
            for (String key : bundled.getKeys(true)) {
                if (bundled.isConfigurationSection(key)) {
                    continue;
                }
                if (!external.contains(key)) {
                    external.set(key, bundled.get(key));
                    changed = true;
                    continue;
                }
                if (shouldRefreshBundledLanguageValue(resourcePath, key, external.getString(key))) {
                    external.set(key, bundled.get(key));
                    changed = true;
                }
            }
            if (changed) {
                external.save(file);
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to merge missing language keys: " + resourcePath, ex);
        }
    }

    static boolean shouldRefreshBundledLanguageValue(String resourcePath, String key, String externalValue) {
        if (resourcePath == null || key == null || externalValue == null) {
            return false;
        }
        if (!resourcePath.replace('\\', '/').endsWith("/gui.yml")) {
            return false;
        }
        return switch (key) {
            case "guide-gui.detail-line" -> externalValue.contains("{trigger}");
            case "guide-gui.effect-phrase-ADD_POTION_SELF",
                 "guide-gui.effect-phrase-ADD_POTION_TARGET" -> !externalValue.contains("{amplifier}");
            case "guide-gui.effect-phrase-SPEED_BOOST" -> !externalValue.contains("{seconds}");
            default -> false;
        };
    }

    private List<String> listBundledLanguageResources() {
        try {
            Path location = Path.of(plugin.getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            if (Files.isDirectory(location)) {
                Path langDir = location.resolve("lang");
                if (!Files.isDirectory(langDir)) {
                    return Collections.emptyList();
                }
                try (var stream = Files.walk(langDir)) {
                    return stream
                            .filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".yml"))
                            .map(location::relativize)
                            .map(path -> path.toString().replace(File.separatorChar, '/'))
                            .sorted()
                            .toList();
                }
            }

            List<String> resources = new ArrayList<>();
            try (JarFile jar = new JarFile(location.toFile())) {
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!entry.isDirectory()
                            && name.startsWith("lang/")
                            && name.toLowerCase(Locale.ROOT).endsWith(".yml")) {
                        resources.add(name);
                    }
                }
            }
            Collections.sort(resources);
            return resources;
        } catch (IOException | URISyntaxException | IllegalArgumentException ex) {
            plugin.getLogger().warning("Unable to list bundled language resources: " + ex.getMessage());
            return Collections.emptyList();
        }
    }

    private String getString(String locale, String fileName, String key, String fallback) {
        String normalizedLocale = normalizeLocale(locale);
        boolean isDefaultLocale = Objects.equals(normalizedLocale, defaultLanguage);
        return resolveString(
                key,
                fallback,
                getConfig(normalizedLocale, fileName),
                getBundledConfig(normalizedLocale, fileName),
                isDefaultLocale ? null : getConfig(defaultLanguage, fileName),
                isDefaultLocale ? null : getBundledConfig(defaultLanguage, fileName)
        );
    }

    private List<String> getStringList(String locale, String fileName, String key) {
        String normalizedLocale = normalizeLocale(locale);
        boolean isDefaultLocale = Objects.equals(normalizedLocale, defaultLanguage);
        return resolveStringList(
                key,
                getConfig(normalizedLocale, fileName),
                getBundledConfig(normalizedLocale, fileName),
                isDefaultLocale ? null : getConfig(defaultLanguage, fileName),
                isDefaultLocale ? null : getBundledConfig(defaultLanguage, fileName)
        );
    }

    static String resolveString(String key, String fallback, YamlConfiguration... configs) {
        for (YamlConfiguration config : configs) {
            if (config == null) {
                continue;
            }
            String value = config.getString(key);
            if (value != null) {
                return value;
            }
        }
        return fallback;
    }

    static List<String> resolveStringList(String key, YamlConfiguration... configs) {
        for (YamlConfiguration config : configs) {
            if (config == null) {
                continue;
            }
            if (config.contains(key)) {
                return config.getStringList(key);
            }
        }
        return Collections.emptyList();
    }

    static String normalizeLocale(String rawLocale) {
        if (rawLocale == null || rawLocale.isBlank()) {
            return "zh_cn";
        }

        String locale = rawLocale.toLowerCase(Locale.ROOT).replace('-', '_');
        if (locale.contains("_")) {
            String[] parts = locale.split("_");
            if (parts.length >= 2) {
                locale = parts[0] + "_" + parts[1];
            }
        }

        if (locale.startsWith("zh_hk") || locale.startsWith("zh_mo")) {
            return "zh_tw";
        }
        if (locale.startsWith("en_") && !"en_us".equals(locale)) {
            return "en_us";
        }
        return locale;
    }

    private YamlConfiguration getConfig(String locale, String fileName) {
        String normalizedLocale = normalizeLocale(locale);
        Map<String, YamlConfiguration> langFiles = loadedLanguages.get(normalizedLocale);
        if (langFiles == null) {
            langFiles = loadLanguage(normalizedLocale);
        }
        YamlConfiguration config = langFiles.get(fileName);
        if (config == null) {
            if (!Objects.equals(normalizedLocale, defaultLanguage)) {
                Map<String, YamlConfiguration> defaultFiles = loadedLanguages.get(defaultLanguage);
                if (defaultFiles != null && defaultFiles.containsKey(fileName)) {
                    return defaultFiles.get(fileName);
                }
            }
            return new YamlConfiguration();
        }
        return config;
    }

    private YamlConfiguration getBundledConfig(String locale, String fileName) {
        if (locale == null) {
            return null;
        }
        Map<String, YamlConfiguration> langFiles = bundledLanguages.get(normalizeLocale(locale));
        if (langFiles == null) {
            langFiles = loadBundledLanguage(locale);
        }
        return langFiles.get(fileName);
    }

    private Map<String, YamlConfiguration> loadLanguage(String locale) {
        String normalizedLocale = normalizeLocale(locale);
        Map<String, YamlConfiguration> langFiles = new ConcurrentHashMap<>();

        for (String fileName : FILE_NAMES) {
            YamlConfiguration config = loadFile(normalizedLocale, fileName);
            if (config != null) {
                langFiles.put(fileName, config);
            }
        }

        if (langFiles.isEmpty() && !Objects.equals(normalizedLocale, defaultLanguage)) {
            plugin.getLogger().warning("No language files found for " + normalizedLocale
                    + ", falling back to " + defaultLanguage);
            Map<String, YamlConfiguration> defaultFiles = loadedLanguages.get(defaultLanguage);
            if (defaultFiles != null) {
                loadedLanguages.put(normalizedLocale, defaultFiles);
                return defaultFiles;
            }
        }

        loadedLanguages.put(normalizedLocale, langFiles);
        return langFiles;
    }

    private Map<String, YamlConfiguration> loadBundledLanguage(String locale) {
        String normalizedLocale = normalizeLocale(locale);
        Map<String, YamlConfiguration> langFiles = new ConcurrentHashMap<>();

        for (String fileName : FILE_NAMES) {
            YamlConfiguration config = loadBundledFile(normalizedLocale, fileName);
            if (config != null) {
                langFiles.put(fileName, config);
            }
        }

        bundledLanguages.put(normalizedLocale, langFiles);
        return langFiles;
    }

    private YamlConfiguration loadFile(String locale, String fileName) {
        String normalizedLocale = normalizeLocale(locale);
        String resourcePath = "lang/" + normalizedLocale + "/" + fileName + ".yml";
        File file = new File(plugin.getDataFolder(), resourcePath);

        if (!file.exists()) {
            try (InputStream resource = plugin.getResource(resourcePath)) {
                if (resource != null) {
                    File parent = file.getParentFile();
                    if (parent != null) {
                        parent.mkdirs();
                    }
                    plugin.saveResource(resourcePath, false);
                } else {
                    return null;
                }
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to inspect language resource: " + resourcePath, ex);
                return null;
            }
        }

        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            YamlConfiguration config = new YamlConfiguration();
            config.load(reader);
            return config;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load language file: " + resourcePath, e);
            return null;
        }
    }

    private YamlConfiguration loadBundledFile(String locale, String fileName) {
        String normalizedLocale = normalizeLocale(locale);
        String resourcePath = "lang/" + normalizedLocale + "/" + fileName + ".yml";

        try (InputStream resource = plugin.getResource(resourcePath)) {
            if (resource == null) {
                return null;
            }
            YamlConfiguration config = new YamlConfiguration();
            try (InputStreamReader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
                config.load(reader);
            }
            return config;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load bundled language file: " + resourcePath, e);
            return null;
        }
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }
}
