package gg.fotia.enchantment.config;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentLimitPolicy;
import gg.fotia.enchantment.item.CodexCraftRarity;
import gg.fotia.enchantment.item.CodexRarityWeights;
import gg.fotia.enchantment.lore.item.EnchantmentSlotLore;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ConfigManager {

    private static final String GRINDSTONE_CONFIG_BLOCK = """

            # ============================================
            # 砂轮
            # EN: Grindstone behavior
            # ============================================
            grindstone:
              # 是否禁用原版砂轮交互；启用后玩家无法打开或使用砂轮
              # EN: Whether to disable vanilla grindstones; when true, players cannot open or use grindstones
              disabled: false
            """;

    private final FotiaEnchantment plugin;
    private YamlConfiguration mainConfig;
    private YamlConfiguration rarityConfig;
    private YamlConfiguration groupsConfig;
    private YamlConfiguration itemsConfig;
    private YamlConfiguration enchantmentBooksConfig;
    private YamlConfiguration limitsConfig;
    private Map<String, YamlConfiguration> guiConfigs = new HashMap<>();
    private List<EnchantmentConfig.ConfigIssue> configIssues = Collections.emptyList();
    private boolean bundledDefaultEnchantmentsInstalled;

    private static final List<String> GUI_CONFIG_IDS = List.of(
            "admin", "fragment-craft", "codex", "enchantment-guide", "disenchant", "anvil-breakthrough");

    public ConfigManager(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载所有配置文件
     */
    public void loadAll() {
        List<EnchantmentConfig.ConfigIssue> issues = new ArrayList<>();
        bundledDefaultEnchantmentsInstalled = false;
        // 保存默认配置文件
        saveDefaultResource("config.yml");
        saveDefaultResource("rarity.yml");
        saveDefaultResource("groups.yml");
        saveDefaultGuiConfigs();
        saveDefaultResource("limits.yml");
        saveDefaultResource("items/custom-items.yml");
        saveDefaultResource("items/enchantment-books.yml");

        // 保存默认原版附魔覆盖配置
        saveDefaultResource("vanilla/sharpness.yml");
        saveDefaultResource("vanilla/mending.yml");
        saveDefaultResource("vanilla/lunge.yml");

        // 保存默认自定义附魔配置
        saveDefaultEnchantments();

        // 加载配置
        mainConfig = loadConfig("config.yml", issues);
        if (appendMissingGrindstoneConfig(mainConfig)) {
            mainConfig = loadConfig("config.yml", issues);
        }
        rarityConfig = loadConfig("rarity.yml", issues);
        groupsConfig = loadConfig("groups.yml", issues);
        loadGuiConfigs(issues);
        limitsConfig = loadConfig("limits.yml", issues);
        refreshAndSaveLimitsConfig(limitsConfig);
        itemsConfig = loadConfig("items/custom-items.yml", issues);
        refreshAndSaveCustomItemsConfig(itemsConfig);
        enchantmentBooksConfig = loadConfig("items/enchantment-books.yml", issues);
        configIssues = List.copyOf(issues);
    }

    /**
     * 重载所有配置文件
     */
    public void reload() {
        List<EnchantmentConfig.ConfigIssue> issues = new ArrayList<>();
        bundledDefaultEnchantmentsInstalled = false;
        saveDefaultGuiConfigs();
        mainConfig = loadConfig("config.yml", issues);
        rarityConfig = loadConfig("rarity.yml", issues);
        groupsConfig = loadConfig("groups.yml", issues);
        loadGuiConfigs(issues);
        limitsConfig = loadConfig("limits.yml", issues);
        refreshAndSaveLimitsConfig(limitsConfig);
        itemsConfig = loadConfig("items/custom-items.yml", issues);
        refreshAndSaveCustomItemsConfig(itemsConfig);
        enchantmentBooksConfig = loadConfig("items/enchantment-books.yml", issues);
        configIssues = List.copyOf(issues);
    }

    /**
     * 加载指定配置文件
     */
    private YamlConfiguration loadConfig(String path, List<EnchantmentConfig.ConfigIssue> issues) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            saveDefaultResource(path);
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (Exception ex) {
            EnchantmentConfig.ConfigIssue issue = EnchantmentConfig.yamlLoadIssue(file, ex);
            issues.add(issue);
            plugin.getLogger().warning(EnchantmentConfig.formatConfigIssue(issue));
        }
        return yaml;
    }

    /**
     * 保存默认资源文件（如果不存在）
     */
    private void saveDefaultResource(String resourcePath) {
        File outFile = new File(plugin.getDataFolder(), resourcePath);
        if (outFile.exists()) {
            return;
        }

        // 确保父目录存在
        File parentDir = outFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                plugin.getLogger().warning("无法在jar中找到资源文件: " + resourcePath);
                return;
            }
            try (OutputStream out = Files.newOutputStream(outFile.toPath())) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存默认配置文件: " + resourcePath);
            e.printStackTrace();
        }
    }

    private boolean appendMissingGrindstoneConfig(YamlConfiguration config) {
        if (config == null || config.contains("grindstone")) {
            return false;
        }

        File file = new File(plugin.getDataFolder(), "config.yml");
        try {
            Files.writeString(file.toPath(), GRINDSTONE_CONFIG_BLOCK, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;
        } catch (IOException ex) {
            plugin.getLogger().warning("无法向 config.yml 追加砂轮配置: " + ex.getMessage());
            return false;
        }
    }

    private void saveDefaultGuiConfigs() {
        for (String id : GUI_CONFIG_IDS) {
            String resourcePath = "gui/" + id + ".yml";
            saveDefaultResource(resourcePath);
            refreshExistingGuiConfig(resourcePath);
        }
    }

    private void refreshExistingGuiConfig(String resourcePath) {
        if (!"gui/enchantment-guide.yml".equals(resourcePath)) {
            return;
        }

        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists() || !file.isFile()) {
            return;
        }

        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String path = "menus.enchantment-guide.items.enchantment.lore";
            List<String> current = yaml.getStringList(path);
            List<String> refreshed = refreshEnchantmentGuideLore(current);
            if (refreshed != current) {
                yaml.set(path, refreshed);
                yaml.save(file);
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("无法更新图鉴 GUI 配置: " + resourcePath + "，" + ex.getMessage());
        }
    }

    static List<String> refreshEnchantmentGuideLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return lore;
        }

        boolean changed = false;
        List<String> refreshed = new ArrayList<>(lore.size() + 2);
        for (String line : lore) {
            if (isPlaceholder(line, "trigger_lines")) {
                refreshed.add("{description_lines}");
                changed = true;
            } else {
                refreshed.add(line);
            }
        }

        boolean hasDescriptionLines = refreshed.stream().anyMatch(line -> isPlaceholder(line, "description_lines"));
        if (!hasDescriptionLines) {
            int effectIndex = indexOfPlaceholder(refreshed, "effect_lines");
            if (effectIndex >= 0) {
                refreshed.add(effectIndex, "");
                refreshed.add(effectIndex, "{description_lines}");
                changed = true;
            }
        }

        return changed ? List.copyOf(refreshed) : lore;
    }

    private static int indexOfPlaceholder(List<String> lines, String placeholder) {
        for (int i = 0; i < lines.size(); i++) {
            if (isPlaceholder(lines.get(i), placeholder)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isPlaceholder(String line, String placeholder) {
        return line != null && line.trim().equals("{" + placeholder + "}");
    }

    private void loadGuiConfigs(List<EnchantmentConfig.ConfigIssue> issues) {
        Map<String, YamlConfiguration> loaded = new HashMap<>();
        for (String id : GUI_CONFIG_IDS) {
            loaded.put(id, loadConfig("gui/" + id + ".yml", issues));
        }
        guiConfigs = Map.copyOf(loaded);
    }

    private void refreshAndSaveLimitsConfig(YamlConfiguration config) {
        if (!refreshLimitsConfig(config)) {
            return;
        }
        try {
            config.save(new File(plugin.getDataFolder(), "limits.yml"));
        } catch (IOException ex) {
            plugin.getLogger().warning("无法保存附魔数量限制迁移配置: limits.yml，" + ex.getMessage());
        }
    }

    static boolean refreshLimitsConfig(YamlConfiguration config) {
        if (config == null || !config.isConfigurationSection("item-groups")) {
            return false;
        }
        if (config.contains("item-groups.spears", true)) {
            return false;
        }
        config.set("item-groups.spears", config.getInt("item-groups.tridents", 6));
        return true;
    }

    private void refreshAndSaveCustomItemsConfig(YamlConfiguration config) {
        if (!refreshCustomItemsConfig(config)) {
            return;
        }
        try {
            config.save(new File(plugin.getDataFolder(), "items/custom-items.yml"));
        } catch (IOException ex) {
            plugin.getLogger().warning("无法保存自定义道具迁移配置: items/custom-items.yml，" + ex.getMessage());
        }
    }

    static boolean refreshCustomItemsConfig(YamlConfiguration config) {
        if (config == null) {
            return false;
        }

        boolean changed = false;
        if (!config.contains("anvil-breakthrough-stone", true)) {
            config.set("anvil-breakthrough-stone.material", "ECHO_SHARD");
            config.set("anvil-breakthrough-stone.custom-model-data", 10030);
            config.set("anvil-breakthrough-stone.item-model", "");
            config.set("anvil-breakthrough-stone.tooltip-style", "");
            config.set("anvil-breakthrough-stone.craftengine-item", "");
            config.set("anvil-breakthrough-stone.glow", true);
            changed = true;
        }

        changed |= ensureDisenchantItem(config, "pure-law-glass", "VANILLA",
                "AMETHYST_SHARD", 10031, true, 1, false, 80, "divine");
        changed |= ensureDisenchantItem(config, "star-scar-prism", "FOTIA",
                "PRISMARINE_CRYSTALS", 10032, true, 3, true, 90, "divine");
        changed |= ensureDisenchantItem(config, "origin-sanctum-core", "ANY",
                "HEART_OF_THE_SEA", 10033, true, 5, true, 100, "divine");
        return changed;
    }

    private static boolean ensureDisenchantItem(YamlConfiguration config,
                                                String id,
                                                String source,
                                                String material,
                                                int customModelData,
                                                boolean glow,
                                                int maxRemove,
                                                boolean selectable,
                                                int successChance,
                                                String maxRarity) {
        String path = "disenchant-stone.items." + id;
        if (config.contains(path, true)) {
            return false;
        }

        config.set(path + ".source", source);
        config.set(path + ".material", material);
        config.set(path + ".custom-model-data", customModelData);
        config.set(path + ".item-model", "");
        config.set(path + ".tooltip-style", "");
        config.set(path + ".craftengine-item", "");
        config.set(path + ".glow", glow);
        config.set(path + ".max-remove", maxRemove);
        config.set(path + ".selectable", selectable);
        config.set(path + ".success-chance", successChance);
        config.set(path + ".destroy-on-fail", false);
        config.set(path + ".keep-level", true);
        config.set(path + ".max-rarity", maxRarity);
        return true;
    }

    /**
     * 保存内置的示例自定义附魔配置。
     */
    private void saveDefaultEnchantments() {
        if (!shouldSaveDefaultEnchantments(plugin.getDataFolder())) {
            return;
        }
        bundledDefaultEnchantmentsInstalled = true;
        for (String resource : listBundledEnchantmentResources()) {
            saveDefaultResource(resource);
        }
    }

    static boolean shouldSaveDefaultEnchantments(File dataFolder) {
        return !new File(dataFolder, "enchantments").exists();
    }

    private List<String> listBundledEnchantmentResources() {
        try {
            Path location = Path.of(plugin.getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            if (Files.isDirectory(location)) {
                Path enchantmentDir = location.resolve("enchantments");
                if (!Files.isDirectory(enchantmentDir)) {
                    return Collections.emptyList();
                }
                try (var stream = Files.walk(enchantmentDir)) {
                    return stream
                            .filter(path -> path.toString().toLowerCase().endsWith(".yml"))
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
                            && name.startsWith("enchantments/")
                            && name.toLowerCase().endsWith(".yml")) {
                        resources.add(name);
                    }
                }
            }
            Collections.sort(resources);
            return resources;
        } catch (IOException | URISyntaxException | IllegalArgumentException ex) {
            plugin.getLogger().warning("无法枚举内置附魔资源: " + ex.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== Getter 方法 ====================

    /**
     * 获取主配置
     */
    public YamlConfiguration getMainConfig() {
        return mainConfig;
    }

    /**
     * 获取稀有度配置
     */
    public YamlConfiguration getRarityConfig() {
        return rarityConfig;
    }

    /**
     * 获取附魔组配置
     */
    public YamlConfiguration getGroupsConfig() {
        return groupsConfig;
    }

    /**
     * 获取自定义道具配置
     */
    public YamlConfiguration getItemsConfig() {
        return itemsConfig;
    }

    /**
     * 获取自定义附魔书外观配置
     */
    public YamlConfiguration getEnchantmentBooksConfig() {
        return enchantmentBooksConfig;
    }

    /**
     * 获取 GUI 配置
     */
    public YamlConfiguration getGuiConfig(String id) {
        YamlConfiguration config = guiConfigs.get(id);
        if (config != null) {
            return config;
        }
        List<EnchantmentConfig.ConfigIssue> issues = new ArrayList<>();
        YamlConfiguration loaded = loadConfig("gui/" + id + ".yml", issues);
        if (!issues.isEmpty()) {
            List<EnchantmentConfig.ConfigIssue> combined = new ArrayList<>(configIssues);
            combined.addAll(issues);
            configIssues = List.copyOf(combined);
        }
        return loaded;
    }

    public List<EnchantmentConfig.ConfigIssue> getConfigIssues() {
        return configIssues;
    }

    public boolean wereBundledDefaultEnchantmentsInstalled() {
        return bundledDefaultEnchantmentsInstalled;
    }

    /**
     * 获取附魔数量限制配置
     */
    public YamlConfiguration getLimitsConfig() {
        return limitsConfig;
    }

    // ==================== 便捷访问方法 ====================

    /**
     * 获取单件装备最大附魔数量
     */
    public int getMaxEnchantmentsPerItem() {
        return mainConfig.getInt("max-enchantments-per-item", 8);
    }

    /**
     * 获取指定材料的单件物品附魔数量上限。
     */
    public int getMaxEnchantmentsForMaterial(Material material) {
        return EnchantmentLimitPolicy.resolveLimit(limitsConfig, material, getMaxEnchantmentsPerItem());
    }

    /**
     * 获取物品 lore 中附魔槽位的显示模式。
     */
    public String getEnchantSlotDisplayMode() {
        return EnchantmentSlotLore.normalizeDisplayMode(
                mainConfig.getString("item-lore.enchant-slots.display-mode", EnchantmentSlotLore.MODE_LINES));
    }

    /**
     * 获取默认语言
     */
    public String getDefaultLanguage() {
        return mainConfig.getString("default-language", "zh_cn");
    }

    /**
     * 是否检查更新
     */
    public boolean isCheckUpdate() {
        return mainConfig.getBoolean("check-update", true);
    }

    public boolean isUpdateCheckerEnabled() {
        return mainConfig.getBoolean("update-checker.enabled", isCheckUpdate());
    }

    public boolean isUpdateCheckOnStartup() {
        return mainConfig.getBoolean("update-checker.check-on-startup", true);
    }

    public int getUpdateCheckDelaySeconds() {
        return Math.max(0, mainConfig.getInt("update-checker.check-delay-seconds", 5));
    }

    public String getUpdateCheckerApiUrl() {
        String owner = mainConfig.getString("update-checker.owner", "Ti-Avanti");
        String repository = mainConfig.getString("update-checker.repository", "FotiaEnchantment");
        String fallback = "https://api.github.com/repos/" + owner + "/" + repository + "/releases/latest";
        return mainConfig.getString("update-checker.api-url", fallback)
                .replace("{owner}", owner)
                .replace("{repository}", repository);
    }

    public String getUpdateCheckerDownloadUrl() {
        String owner = mainConfig.getString("update-checker.owner", "Ti-Avanti");
        String repository = mainConfig.getString("update-checker.repository", "FotiaEnchantment");
        String fallback = "https://github.com/" + owner + "/" + repository + "/releases/latest";
        return mainConfig.getString("update-checker.download-url", fallback)
                .replace("{owner}", owner)
                .replace("{repository}", repository);
    }

    public boolean isUpdateNotifyConsole() {
        return mainConfig.getBoolean("update-checker.notify.console", true);
    }

    public boolean isUpdateNotifyOps() {
        return mainConfig.getBoolean("update-checker.notify.ops", true);
    }

    /**
     * 附魔台是否可获取自定义附魔
     */
    public boolean isEnchantingTableEnabled() {
        return mainConfig.getBoolean("obtain.enchanting-table", true);
    }

    /**
     * 铁砧合成是否启用
     */
    public boolean isAnvilEnabled() {
        return mainConfig.getBoolean("obtain.anvil", true);
    }

    /**
     * 村民交易是否启用
     */
    public boolean isVillagerTradeEnabled() {
        return mainConfig.getBoolean("obtain.villager-trade", true);
    }

    /**
     * 砂轮是否被禁用。
     */
    public boolean isGrindstoneDisabled() {
        return mainConfig.getBoolean("grindstone.disabled", false);
    }

    public int getEnchantingTableCustomRolls() {
        return Math.max(0, mainConfig.getInt("enchanting-table.custom-rolls", 3));
    }

    public double getEnchantingTableBaseChance() {
        return mainConfig.getDouble("enchanting-table.base-custom-chance", 0.35);
    }

    public double getEnchantingTableChancePerLevel() {
        return mainConfig.getDouble("enchanting-table.custom-chance-per-level", 0.03);
    }

    public double getEnchantingTableMaxChance() {
        return mainConfig.getDouble("enchanting-table.max-custom-chance", 0.95);
    }

    /**
     * 获取效果检查间隔（tick）
     */
    public int getEffectCheckInterval() {
        return mainConfig.getInt("performance.effect-check-interval", 1);
    }

    /**
     * 获取每tick最大效果执行数
     */
    public int getMaxEffectsPerTick() {
        return mainConfig.getInt("performance.max-effects-per-tick", 50);
    }

    public long getItemValidityCheckInterval() {
        return Math.max(20L, mainConfig.getLong("performance.item-validity-check-interval", 40L));
    }

    /**
     * 获取星芒魔典合成所需碎片数量
     */
    public int getStellarisCodexFragmentCost() {
        return mainConfig.getInt("stellaris-codex.fragment-cost", 5);
    }

    /**
     * 碎片合成星芒魔典始终随机品质。保留入口用于兼容旧配置，但不再读取固定品质。
     */
    public String getStellarisCodexCraftRarity() {
        return CodexCraftRarity.RANDOM;
    }

    /**
     * 获取碎片合成星芒魔典时各品质的随机权重。
     */
    public Map<String, Integer> getStellarisCodexRarityWeights() {
        return CodexRarityWeights.resolve(mainConfig, rarityConfig);
    }
}
