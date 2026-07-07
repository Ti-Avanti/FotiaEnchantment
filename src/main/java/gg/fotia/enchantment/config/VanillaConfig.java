package gg.fotia.enchantment.config;

import gg.fotia.enchantment.FotiaEnchantment;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 原版附魔覆盖配置加载器
 * <p>扫描 plugins/FotiaEnchantment/vanilla/ 目录下所有 yml 文件，
 * 将配置解析为 VanillaOverride 数据结构供 VanillaManager 使用。
 */
public class VanillaConfig {

    private final FotiaEnchantment plugin;
    /** 附魔名(小写) → 覆盖配置 */
    private final Map<String, VanillaOverride> overrides = new HashMap<>();
    private static final Map<String, VanillaText> DEFAULT_TEXTS = createDefaultTexts();
    private static final String GENERIC_VANILLA_DESCRIPTION = "原版附魔，具体效果遵循服务器当前 Minecraft 版本。";

    public VanillaConfig(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载 vanilla/ 目录下所有 yml 文件
     */
    public void loadAll() {
        overrides.clear();
        File vanillaDir = new File(plugin.getDataFolder(), "vanilla");
        ensureVanillaDirectory(vanillaDir);
        ensureAllVanillaFiles(vanillaDir);
        if (!vanillaDir.exists() || !vanillaDir.isDirectory()) {
            return;
        }

        File[] files = vanillaDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            loadFile(file);
        }

        plugin.getLogger().info("已加载 " + overrides.size() + " 个原版附魔覆盖配置");
    }

    /**
     * 确保 vanilla 配置目录存在。
     */
    private void ensureVanillaDirectory(File vanillaDir) {
        if (vanillaDir.exists()) {
            return;
        }
        if (!vanillaDir.mkdirs()) {
            plugin.getLogger().warning("无法创建原版附魔配置目录: " + vanillaDir.getPath());
        }
    }

    /**
     * 按当前服务端注册表自动补齐所有 minecraft 命名空间的原版附魔配置。
     */
    private void ensureAllVanillaFiles(File vanillaDir) {
        List<Enchantment> vanillaEnchantments = Registry.ENCHANTMENT.stream()
                .filter(enchant -> "minecraft".equals(enchant.getKey().getNamespace()))
                .sorted((left, right) -> left.getKey().getKey().compareTo(right.getKey().getKey()))
                .toList();

        int created = 0;
        for (Enchantment enchantment : vanillaEnchantments) {
            String key = enchantment.getKey().getKey().toLowerCase(Locale.ROOT);
            File file = new File(vanillaDir, key + ".yml");
            if (file.exists()) {
                continue;
            }
            try {
                Files.writeString(file.toPath(), defaultVanillaConfig(enchantment), StandardCharsets.UTF_8);
                created++;
            } catch (IOException ex) {
                plugin.getLogger().severe("无法生成原版附魔配置文件: vanilla/" + key + ".yml");
                ex.printStackTrace();
            }
        }
        if (created > 0) {
            plugin.getLogger().info("已生成 " + created + " 个缺失的原版附魔配置文件");
        }
    }

    /**
     * 加载单个覆盖配置文件
     */
    private void loadFile(File file) {
        String enchantName = file.getName().replace(".yml", "").toLowerCase(Locale.ROOT);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        VanillaOverride override = new VanillaOverride(enchantName);
        override.setDisabled(config.getBoolean("disabled", false));
        override.setMaxLevel(config.getInt("max-level", -1));
        override.setEnchantingTableMaxLevel(config.getInt("enchanting-table-max-level", -1));
        override.setConflicts(config.getStringList("conflicts"));
        List<String> configuredApplicableItems = config.getStringList("applicable-items");
        List<String> migratedApplicableItems = migrateApplicableItems(enchantName, configuredApplicableItems);
        override.setApplicableItems(migratedApplicableItems);
        override.setEnchantingTableWeight(config.getInt("enchanting-table-weight", -1));
        String configuredDisplayName = config.getString("display-name", defaultDisplayName(enchantName));
        String migratedDisplayName = migrateDisplayName(enchantName, configuredDisplayName);
        override.setDisplayName(migratedDisplayName);
        List<String> description = config.getStringList("description");
        List<String> configuredDescription = description.isEmpty() ? defaultDescription(enchantName) : description;
        List<String> migratedDescription = migrateDescription(enchantName, configuredDescription);
        override.setDescription(migratedDescription);

        boolean migrated = false;
        if (!migratedApplicableItems.equals(configuredApplicableItems)) {
            config.set("applicable-items", migratedApplicableItems);
            migrated = true;
        }
        if (!Objects.equals(migratedDisplayName, configuredDisplayName)) {
            config.set("display-name", migratedDisplayName);
            migrated = true;
        }
        if (!migratedDescription.equals(configuredDescription)) {
            config.set("description", migratedDescription);
            migrated = true;
        }
        if (migrated) {
            saveMigratedFile(file, config);
        }

        overrides.put(enchantName, override);
    }

    private void saveMigratedFile(File file, YamlConfiguration config) {
        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("无法保存原版附魔迁移配置: " + file.getPath() + "，" + ex.getMessage());
        }
    }

    /**
     * 获取指定原版附魔的覆盖配置
     *
     * @param enchantName 原版附魔名（如 sharpness）
     * @return 覆盖配置，不存在返回 null
     */
    public VanillaOverride getOverride(String enchantName) {
        if (enchantName == null) {
            return null;
        }
        return overrides.get(enchantName.toLowerCase(Locale.ROOT));
    }

    /**
     * 获取所有覆盖配置（不可修改视图）
     */
    public Map<String, VanillaOverride> getAllOverrides() {
        return Collections.unmodifiableMap(overrides);
    }

    /**
     * 重载所有覆盖配置
     */
    public void reload() {
        loadAll();
    }

    static List<String> migrateApplicableItems(String enchantName, List<String> applicableItems) {
        if (applicableItems == null) {
            return new ArrayList<>();
        }
        if (!"sharpness".equals(normalizeEnchantKey(enchantName))) {
            return applicableItems;
        }
        if (applicableItems.stream().anyMatch(item -> "SPEAR".equals(normalizeToken(item)))) {
            return applicableItems;
        }
        List<String> legacyDefault = List.of("SWORD", "AXE", "MACE");
        List<String> current = applicableItems.stream()
                .map(VanillaConfig::normalizeToken)
                .toList();
        if (!current.equals(legacyDefault)) {
            return applicableItems;
        }

        List<String> migrated = new ArrayList<>(applicableItems);
        migrated.add("SPEAR");
        return migrated;
    }

    static String migrateDisplayName(String enchantName, String displayName) {
        String key = normalizeEnchantKey(enchantName);
        if (!"lunge".equals(key)) {
            return displayName;
        }
        if (displayName == null || displayName.isBlank() || displayName.equals(humanizeVanillaKey(key))) {
            return defaultDisplayName(key);
        }
        return displayName;
    }

    static List<String> migrateDescription(String enchantName, List<String> description) {
        String key = normalizeEnchantKey(enchantName);
        if (!"lunge".equals(key)) {
            return description == null ? new ArrayList<>() : description;
        }
        if (description == null || description.isEmpty()) {
            return defaultDescription(key);
        }
        if (description.size() == 1 && GENERIC_VANILLA_DESCRIPTION.equals(description.get(0))) {
            return defaultDescription(key);
        }
        return description;
    }

    private static String normalizeEnchantKey(String enchantName) {
        return enchantName == null ? "" : enchantName.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeToken(String token) {
        return token == null ? "" : token.trim().toUpperCase(Locale.ROOT);
    }

    // ==================== 内部数据结构 ====================

    /**
     * 原版附魔覆盖数据
     */
    public static class VanillaOverride {
        private final String name;
        private boolean disabled;
        private int maxLevel;
        private int enchantingTableMaxLevel;
        private List<String> conflicts;
        private List<String> applicableItems;
        private int enchantingTableWeight;
        private String displayName;
        private List<String> description;

        public VanillaOverride(String name) {
            this.name = name;
            this.disabled = false;
            this.maxLevel = -1;
            this.enchantingTableMaxLevel = -1;
            this.conflicts = new ArrayList<>();
            this.applicableItems = new ArrayList<>();
            this.enchantingTableWeight = -1;
            this.displayName = name;
            this.description = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public boolean isDisabled() {
            return disabled;
        }

        public void setDisabled(boolean disabled) {
            this.disabled = disabled;
        }

        public int getMaxLevel() {
            return maxLevel;
        }

        public void setMaxLevel(int maxLevel) {
            this.maxLevel = maxLevel;
        }

        public int getEnchantingTableMaxLevel() {
            return enchantingTableMaxLevel;
        }

        public void setEnchantingTableMaxLevel(int enchantingTableMaxLevel) {
            this.enchantingTableMaxLevel = enchantingTableMaxLevel;
        }

        public List<String> getConflicts() {
            return conflicts;
        }

        public void setConflicts(List<String> conflicts) {
            this.conflicts = conflicts != null ? conflicts : new ArrayList<>();
        }

        public List<String> getApplicableItems() {
            return applicableItems;
        }

        public void setApplicableItems(List<String> applicableItems) {
            this.applicableItems = applicableItems != null ? applicableItems : new ArrayList<>();
        }

        public int getEnchantingTableWeight() {
            return enchantingTableWeight;
        }

        public void setEnchantingTableWeight(int enchantingTableWeight) {
            this.enchantingTableWeight = enchantingTableWeight;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName != null && !displayName.isBlank() ? displayName : name;
        }

        public List<String> getDescription() {
            return description;
        }

        public void setDescription(List<String> description) {
            this.description = description != null ? description : new ArrayList<>();
        }
    }

    private String defaultVanillaConfig(Enchantment enchantment) {
        String key = enchantment.getKey().getKey().toLowerCase(Locale.ROOT);
        StringJoiner descriptionLines = new StringJoiner(System.lineSeparator());
        for (String line : defaultDescription(key)) {
            descriptionLines.add("  - \"" + line.replace("\"", "\\\"") + "\"");
        }
        return """
                # ============================================
                # 原版附魔覆盖: %s
                # 默认值不改变原版行为；按需修改后执行 /fe reload 生效。
                # ============================================

                # 是否禁用此原版附魔
                # 设为 true 后该附魔会从附魔台结果和铁砧结果中移除
                disabled: false

                # 覆盖最大等级（-1 表示不修改，使用原版默认值）
                # 原版默认最大等级: %d
                max-level: -1

                # 附魔台最高可生成等级（-1 表示使用原版默认等级）
                # 该值仍不会超过 max-level；例如 max-level: 10 且这里为 5 时，铁砧/指令可保留 X，附魔台最多生成 V
                enchanting-table-max-level: -1

                # 额外冲突的附魔ID列表（填写 minecraft 命名空间下的键名，例如 infinity）
                conflicts: []

                # 覆盖适用物品类型（留空表示使用原版 canEnchantItem 判定）
                # 这里按物品枚举名片段匹配，例如 SWORD、AXE、BOOTS
                applicable-items: []

                # 附魔台候选权重（-1 表示不干预附魔台随机；0 表示不作为附魔台候选）
                # 仅在至少一个原版附魔配置了非 -1 权重时参与重新加权
                enchanting-table-weight: -1

                # lore 接管时显示的原版附魔名称，支持 MiniMessage 和 &/§ 旧颜色码
                display-name: "%s"

                # lore 接管时显示在名称下一行的描述，支持 MiniMessage 和 &/§ 旧颜色码
                description:
                %s
                """.formatted(key, enchantment.getMaxLevel(), defaultDisplayName(key), descriptionLines);
    }

    private static String defaultDisplayName(String key) {
        VanillaText text = DEFAULT_TEXTS.get(key);
        return text != null ? text.name() : humanizeVanillaKey(key);
    }

    private static List<String> defaultDescription(String key) {
        VanillaText text = DEFAULT_TEXTS.get(key);
        if (text != null) {
            return text.description();
        }
        return List.of(GENERIC_VANILLA_DESCRIPTION);
    }

    private static String humanizeVanillaKey(String key) {
        if (key == null || key.isBlank()) {
            return "原版附魔";
        }
        String[] parts = key.split("_");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
        }
        return words.isEmpty() ? key : String.join(" ", words);
    }

    private static Map<String, VanillaText> createDefaultTexts() {
        Map<String, VanillaText> texts = new HashMap<>();
        texts.put("protection", text("保护", "减少多数来源造成的伤害。"));
        texts.put("fire_protection", text("火焰保护", "减少火焰伤害，并缩短燃烧时间。"));
        texts.put("feather_falling", text("摔落保护", "减少摔落伤害。"));
        texts.put("blast_protection", text("爆炸保护", "减少爆炸伤害和击退。"));
        texts.put("projectile_protection", text("弹射物保护", "减少箭矢、三叉戟等弹射物伤害。"));
        texts.put("respiration", text("水下呼吸", "延长水下呼吸时间，并降低窒息概率。"));
        texts.put("aqua_affinity", text("水下速掘", "提高水下挖掘速度。"));
        texts.put("thorns", text("荆棘", "受到攻击时有概率反伤攻击者。"));
        texts.put("depth_strider", text("深海探索者", "提高水下移动速度。"));
        texts.put("frost_walker", text("冰霜行者", "行走时将脚下水面冻结成霜冰。"));
        texts.put("binding_curse", text("绑定诅咒", "穿戴后无法正常脱下，死亡后才会移除。"));
        texts.put("soul_speed", text("灵魂疾行", "提高在灵魂沙和灵魂土上的移动速度。"));
        texts.put("swift_sneak", text("迅捷潜行", "提高潜行移动速度。"));
        texts.put("sharpness", text("锋利", "提高近战攻击造成的伤害。"));
        texts.put("smite", text("亡灵杀手", "提高对亡灵生物造成的伤害。"));
        texts.put("bane_of_arthropods", text("节肢杀手", "提高对节肢生物造成的伤害，并附加缓慢。"));
        texts.put("knockback", text("击退", "提高近战攻击造成的击退距离。"));
        texts.put("fire_aspect", text("火焰附加", "近战攻击会点燃目标。"));
        texts.put("looting", text("抢夺", "提高生物掉落数量和稀有掉落概率。"));
        texts.put("sweeping_edge", text("横扫之刃", "提高横扫攻击造成的伤害。"));
        texts.put("efficiency", text("效率", "提高工具挖掘速度。"));
        texts.put("silk_touch", text("精准采集", "让方块尽量掉落自身。"));
        texts.put("unbreaking", text("耐久", "降低物品消耗耐久的概率。"));
        texts.put("fortune", text("时运", "提高部分方块的掉落数量。"));
        texts.put("power", text("力量", "提高弓箭伤害。"));
        texts.put("punch", text("冲击", "提高弓箭击退距离。"));
        texts.put("flame", text("火矢", "射出的箭会点燃目标。"));
        texts.put("infinity", text("无限", "背包有一支普通箭时可无限射击普通箭。"));
        texts.put("luck_of_the_sea", text("海之眷顾", "提高钓鱼获得宝藏的概率。"));
        texts.put("lure", text("饵钓", "缩短鱼上钩所需时间。"));
        texts.put("loyalty", text("忠诚", "投掷出的三叉戟会飞回持有者。"));
        texts.put("impaling", text("穿刺", "提高三叉戟对水生目标或潮湿目标的伤害。"));
        texts.put("riptide", text("激流", "在水中或雨中投掷三叉戟会带动玩家冲刺。"));
        texts.put("channeling", text("引雷", "雷暴天气下三叉戟命中会召唤闪电。"));
        texts.put("multishot", text("多重射击", "弩一次发射三支弹射物。"));
        texts.put("quick_charge", text("快速装填", "缩短弩的装填时间。"));
        texts.put("piercing", text("穿透", "弩箭可以穿透多个实体。"));
        texts.put("mending", text("经验修补", "拾取经验时修复带有该附魔的物品。"));
        texts.put("vanishing_curse", text("消失诅咒", "玩家死亡后物品会直接消失。"));
        texts.put("density", text("致密", "提高重锤下落攻击造成的伤害。"));
        texts.put("breach", text("破甲", "重锤攻击会削弱目标护甲减伤。"));
        texts.put("wind_burst", text("风爆", "重锤下落攻击命中后将玩家向上弹起。"));
        texts.put("lunge", text("突进", "提高持矛突进攻击造成的伤害。"));
        return Collections.unmodifiableMap(texts);
    }

    private static VanillaText text(String name, String description) {
        return new VanillaText(name, List.of(description));
    }

    private record VanillaText(String name, List<String> description) {
    }
}
