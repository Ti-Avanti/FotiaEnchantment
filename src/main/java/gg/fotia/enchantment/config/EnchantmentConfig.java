package gg.fotia.enchantment.config;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.EnchantmentData.ActionConfig;
import gg.fotia.enchantment.core.EnchantmentData.ConditionConfig;
import gg.fotia.enchantment.core.EnchantmentData.EffectBlock;
import gg.fotia.enchantment.core.EnchantmentData.ObtainSettings;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 附魔配置加载器 - 从 YAML 文件解析附魔配置为 {@link EnchantmentData} 对象。
 *
 * <p>扫描 {@code plugins/FotiaEnchantment/enchantments/} 下所有子目录的 .yml
 * 文件，每个文件对应一个附魔。文件所在的子目录名会被记录为 category。
 */
public class EnchantmentConfig {

    private final FotiaEnchantment plugin;

    /** 已加载附魔（按 ID 索引，保持加载顺序） */
    private final Map<String, EnchantmentData> enchantments = new LinkedHashMap<>();
    private final Map<String, File> sourceFiles = new HashMap<>();

    /** 物品类别标签 → 实际 Material 列表 */
    private static final Map<String, List<Material>> CATEGORY_MATERIALS = buildCategoryMaterials();

    public EnchantmentConfig(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    // ==================== 公共 API ====================

    /**
     * 加载全部附魔配置：先确保资源已被释放到数据目录，再扫描数据目录。
     */
    public void loadAll() {
        enchantments.clear();
        sourceFiles.clear();

        File baseDir = new File(plugin.getDataFolder(), "enchantments");
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            plugin.getLogger().warning("附魔目录不存在: " + baseDir.getAbsolutePath());
            return;
        }

        int count = 0;
        File[] subDirs = baseDir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                File[] files = subDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
                if (files == null) {
                    continue;
                }
                for (File file : files) {
                    EnchantmentData data = loadEnchantment(file);
                    if (data == null) {
                        continue;
                    }
                    if (data.getCategory() == null || data.getCategory().isEmpty()) {
                        data.setCategory(subDir.getName());
                    }
                    if (enchantments.containsKey(data.getId())) {
                        plugin.getLogger().warning("附魔 ID 重复，已忽略: "
                                + data.getId() + " (来自 " + file.getAbsolutePath() + ")");
                        continue;
                    }
                    sourceFiles.put(data.getId(), file);
                    enchantments.put(data.getId(), data);
                    count++;
                }
            }
        }

        // 兼容直接放在 enchantments/ 根目录下的 yml 文件
        File[] rootFiles = baseDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (rootFiles != null) {
            for (File file : rootFiles) {
                EnchantmentData data = loadEnchantment(file);
                if (data == null) {
                    continue;
                }
                if (enchantments.containsKey(data.getId())) {
                    plugin.getLogger().warning("附魔 ID 重复，已忽略: "
                            + data.getId() + " (来自 " + file.getAbsolutePath() + ")");
                    continue;
                }
                sourceFiles.put(data.getId(), file);
                enchantments.put(data.getId(), data);
                count++;
            }
        }

        plugin.getLogger().info("已加载 " + count + " 个自定义附魔配置");
    }

    /**
     * 重新加载所有附魔配置
     */
    public void reload() {
        loadAll();
    }

    /**
     * 获取所有已加载的附魔（不可修改视图）
     */
    public Collection<EnchantmentData> getEnchantments() {
        return Collections.unmodifiableCollection(enchantments.values());
    }

    /**
     * 按 ID 获取附魔
     */
    public EnchantmentData getEnchantment(String id) {
        if (id == null) {
            return null;
        }
        return enchantments.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean setEnabled(String id, boolean enabled) {
        if (id == null) {
            return false;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        File sourceFile = sourceFiles.get(normalized);
        if (!saveEnabledFlag(sourceFile, enabled)) {
            return false;
        }
        EnchantmentData data = enchantments.get(normalized);
        if (data != null) {
            data.setEnabled(enabled);
        }
        return true;
    }

    static boolean saveEnabledFlag(File file, boolean enabled) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("enabled", enabled);
        try {
            yaml.save(file);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * 从单个 YAML 文件加载一个 EnchantmentData
     */
    public EnchantmentData loadEnchantment(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }
        YamlConfiguration yaml;
        try {
            yaml = YamlConfiguration.loadConfiguration(file);
        } catch (Exception ex) {
            plugin.getLogger().warning("无法加载附魔配置文件 " + file.getName() + ": " + ex.getMessage());
            return null;
        }
        return parse(yaml, file);
    }

    // ==================== 解析 ====================

    private EnchantmentData parse(YamlConfiguration yaml, File file) {
        EnchantmentData data = new EnchantmentData();

        // ID：优先使用配置中的值，缺省则用文件名（不含扩展名）
        String id = yaml.getString("id", null);
        if (id == null || id.isEmpty()) {
            String name = file.getName();
            int dot = name.lastIndexOf('.');
            id = dot > 0 ? name.substring(0, dot) : name;
        }
        data.setId(id.toLowerCase(Locale.ROOT));

        data.setEnabled(yaml.getBoolean("enabled", true));
        data.setCurse(yaml.getBoolean("curse", false));
        data.setMaxLevel(Math.max(1, yaml.getInt("max-level", 1)));
        data.setRarity(yaml.getString("rarity"));
        data.setGroup(yaml.getString("group"));
        data.setCategory(yaml.getString("category"));

        // applicable-items
        data.setApplicableItems(parseMaterials(yaml.getStringList("applicable-items")));

        // conflicts
        List<String> conflicts = new ArrayList<>();
        for (String s : yaml.getStringList("conflicts")) {
            if (s != null && !s.isEmpty()) {
                conflicts.add(s.toLowerCase(Locale.ROOT));
            }
        }
        data.setConflicts(conflicts);

        // obtain
        data.setObtain(parseObtain(yaml));

        // codex-pools
        data.setCodexPools(parseCodexPools(yaml.getConfigurationSection("codex-pools")));

        // effects
        data.setEffects(parseEffects(yaml.getList("effects"), file));

        return data;
    }

    private ObtainSettings parseObtain(YamlConfiguration yaml) {
        ObtainSettings obtain = new ObtainSettings();
        ConfigurationSection sec = yaml.getConfigurationSection("obtain");
        if (sec != null) {
            obtain.setEnchantingTable(sec.getBoolean("enchanting-table", true));
            obtain.setAnvil(sec.getBoolean("anvil", true));
            obtain.setVillagerTrade(sec.getBoolean("villager-trade", true));
        }
        // 附魔台权重：兼容根级 enchanting-table-weight 与 obtain.enchanting-table-weight
        int weight = yaml.getInt("enchanting-table-weight", -1);
        if (weight < 0 && sec != null) {
            weight = sec.getInt("enchanting-table-weight", 10);
        }
        if (weight < 0) {
            weight = 10;
        }
        obtain.setEnchantingTableWeight(weight);

        // 村民交易价格区间
        List<Integer> priceRange = yaml.getIntegerList("villager-trade-price-range");
        if ((priceRange == null || priceRange.size() < 2) && sec != null) {
            priceRange = sec.getIntegerList("villager-trade-price-range");
        }
        if (priceRange != null && priceRange.size() >= 2) {
            obtain.setVillagerTradePriceRange(new int[]{priceRange.get(0), priceRange.get(1)});
        }
        return obtain;
    }

    private Map<String, Integer> parseCodexPools(ConfigurationSection section) {
        Map<String, Integer> pools = new LinkedHashMap<>();
        if (section == null) {
            return pools;
        }
        for (String key : section.getKeys(false)) {
            int weight = section.getInt(key, 0);
            if (weight > 0) {
                pools.put(key.toLowerCase(Locale.ROOT), weight);
            }
        }
        return pools;
    }

    @SuppressWarnings("unchecked")
    private List<EffectBlock> parseEffects(List<?> rawEffects, File file) {
        List<EffectBlock> result = new ArrayList<>();
        if (rawEffects == null) {
            return result;
        }
        for (Object item : rawEffects) {
            if (!(item instanceof Map<?, ?> raw)) {
                continue;
            }
            Map<String, Object> map = (Map<String, Object>) raw;

            EffectBlock block = new EffectBlock();
            Object trigger = map.get("trigger");
            if (trigger == null) {
                plugin.getLogger().warning("附魔 " + file.getName()
                        + " 中存在缺少 trigger 字段的 effect 块，已跳过");
                continue;
            }
            block.setTrigger(String.valueOf(trigger));
            block.setCooldown(toInt(map.get("cooldown"), 0));

            block.setConditions(parseConditions(map.get("conditions")));
            block.setActions(parseActions(map.get("actions")));

            result.add(block);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<ConditionConfig> parseConditions(Object raw) {
        List<ConditionConfig> result = new ArrayList<>();
        if (!(raw instanceof List<?> list)) {
            return result;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                continue;
            }
            Map<String, Object> map = (Map<String, Object>) rawMap;
            Object type = map.get("type");
            if (type == null) {
                continue;
            }
            ConditionConfig cfg = new ConditionConfig();
            cfg.setType(String.valueOf(type));
            Object value = map.get("value");
            if (value != null) {
                cfg.setValue(String.valueOf(value));
            }
            Map<String, Object> extras = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String k = entry.getKey();
                if ("type".equals(k) || "value".equals(k)) {
                    continue;
                }
                extras.put(k, entry.getValue());
            }
            cfg.setExtraParams(extras);
            result.add(cfg);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<ActionConfig> parseActions(Object raw) {
        List<ActionConfig> result = new ArrayList<>();
        if (!(raw instanceof List<?> list)) {
            return result;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                continue;
            }
            Map<String, Object> map = (Map<String, Object>) rawMap;
            Object type = map.get("type");
            if (type == null) {
                continue;
            }
            ActionConfig cfg = new ActionConfig();
            cfg.setType(String.valueOf(type));
            Object value = map.get("value");
            if (value != null) {
                cfg.setValue(String.valueOf(value));
            }
            Map<String, Object> extras = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String k = entry.getKey();
                if ("type".equals(k) || "value".equals(k)) {
                    continue;
                }
                extras.put(k, entry.getValue());
            }
            cfg.setExtraParams(extras);
            result.add(cfg);
        }
        return result;
    }

    private List<Material> parseMaterials(List<String> tokens) {
        List<Material> result = new ArrayList<>();
        if (tokens == null) {
            return result;
        }
        for (String token : tokens) {
            if (token == null || token.isEmpty()) {
                continue;
            }
            String upper = token.trim().toUpperCase(Locale.ROOT);

            // 优先匹配类别标签
            List<Material> mats = CATEGORY_MATERIALS.get(upper);
            if (mats != null) {
                for (Material m : mats) {
                    if (!result.contains(m)) {
                        result.add(m);
                    }
                }
                continue;
            }

            // 直接匹配 Material 名
            try {
                Material m = Material.valueOf(upper);
                if (!result.contains(m)) {
                    result.add(m);
                }
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("未知的 applicable-items 项: " + token);
            }
        }
        return result;
    }

    private static int toInt(Object obj, int defaultValue) {
        if (obj instanceof Number n) {
            return n.intValue();
        }
        if (obj == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(obj).trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    // ==================== 类别 → 材质表 ====================

    private static Map<String, List<Material>> buildCategoryMaterials() {
        Map<String, List<Material>> map = new HashMap<>();
        map.put("SWORD", Arrays.asList(
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        map.put("AXE", Arrays.asList(
                Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE));
        map.put("PICKAXE", Arrays.asList(
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE));
        map.put("SHOVEL", Arrays.asList(
                Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL));
        map.put("HOE", Arrays.asList(
                Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
                Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE));
        map.put("BOW", Collections.singletonList(Material.BOW));
        map.put("CROSSBOW", Collections.singletonList(Material.CROSSBOW));
        map.put("TRIDENT", Collections.singletonList(Material.TRIDENT));
        map.put("FISHING_ROD", Collections.singletonList(Material.FISHING_ROD));
        map.put("SHIELD", Collections.singletonList(Material.SHIELD));
        map.put("ELYTRA", Collections.singletonList(Material.ELYTRA));
        map.put("HELMET", Arrays.asList(
                Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET,
                Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET,
                Material.TURTLE_HELMET));
        map.put("CHESTPLATE", Arrays.asList(
                Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE,
                Material.IRON_CHESTPLATE, Material.GOLDEN_CHESTPLATE,
                Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE));
        map.put("LEGGINGS", Arrays.asList(
                Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS,
                Material.IRON_LEGGINGS, Material.GOLDEN_LEGGINGS,
                Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS));
        map.put("BOOTS", Arrays.asList(
                Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
                Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS));
        return map;
    }
}
