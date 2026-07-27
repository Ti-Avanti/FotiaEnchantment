package gg.fotia.enchantment.core;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 附魔数据模型 - 存储一个附魔的完整配置信息
 *
 * <p>该对象由 YAML 配置加载器从单个附魔配置文件解析得到，
 * 表示一种自定义附魔的全部静态数据，包括基础属性、获取方式以及效果管道配置。
 *
 * <p>集合字段在 setter 中固化为不可变实例并预计算查询结构
 * (适用材质 EnumSet、归一化冲突 Set), 供主线程与数据包线程安全共享。
 */
public class EnchantmentData {

    /** 附魔ID（通常为文件名，不含扩展名） */
    private String id;

    /** 是否启用该附魔 (管理 GUI 可在运行期切换, 需跨线程可见) */
    private volatile boolean enabled = true;

    private boolean curse;

    /** 最大等级 */
    private int maxLevel = 1;

    /** 稀有度（引用 rarity.yml 中的 key） */
    private String rarity;

    /** 所属附魔组（引用 groups.yml 中的 key） */
    private String group;

    /** 适用物品类型 (保留配置顺序) */
    private List<Material> applicableItems = List.of();

    /** 适用物品类型的 O(1) 查询集合 */
    private Set<Material> applicableItemSet = Collections.emptySet();

    /** 冲突附魔ID列表 (原始配置值) */
    private List<String> conflicts = List.of();

    /** 预归一化的冲突自定义ID集合 (去掉 fotia: 前缀、小写) */
    private Set<String> normalizedConflictIds = Set.of();

    /** 冲突引用中带命名空间的完整 key (小写) */
    private Set<String> conflictNamespacedKeys = Set.of();

    /** 冲突引用中不带命名空间的简单 key (小写) */
    private Set<String> conflictSimpleKeys = Set.of();

    /** 获取方式配置 */
    private ObtainSettings obtain = new ObtainSettings();

    /** 星芒魔典抽取池权重 {稀有度: 权重} */
    private Map<String, Integer> codexPools = Map.of();

    /** 效果管道配置列表 */
    private List<EffectBlock> effects = List.of();

    /** 类别（melee/ranged/armor/tools/universal 等） */
    private String category;

    // ==================== Getter / Setter ====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isCurse() {
        return curse;
    }

    public void setCurse(boolean curse) {
        this.curse = curse;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public List<Material> getApplicableItems() {
        return applicableItems;
    }

    public void setApplicableItems(List<Material> applicableItems) {
        if (applicableItems == null || applicableItems.isEmpty()) {
            this.applicableItems = List.of();
            this.applicableItemSet = Collections.emptySet();
            return;
        }
        this.applicableItems = List.copyOf(applicableItems);
        this.applicableItemSet = Collections.unmodifiableSet(EnumSet.copyOf(applicableItems));
    }

    /**
     * O(1) 判断材质是否在适用列表中。
     * 未配置适用列表时返回 false, "空列表=适用所有"的语义由调用方处理。
     */
    public boolean isApplicableTo(Material material) {
        return material != null && applicableItemSet.contains(material);
    }

    public List<String> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<String> conflicts) {
        if (conflicts == null || conflicts.isEmpty()) {
            this.conflicts = List.of();
            this.normalizedConflictIds = Set.of();
            this.conflictNamespacedKeys = Set.of();
            this.conflictSimpleKeys = Set.of();
            return;
        }
        this.conflicts = List.copyOf(conflicts);
        // 预归一化, 让热路径冲突判定退化为 Set.contains
        Set<String> customIds = new HashSet<>();
        Set<String> namespaced = new HashSet<>();
        Set<String> simple = new HashSet<>();
        for (String reference : this.conflicts) {
            if (reference == null) {
                continue;
            }
            String raw = reference.trim().toLowerCase(Locale.ROOT);
            if (raw.isEmpty()) {
                continue;
            }
            String custom = EnchantmentConflictPolicy.normalizeCustomId(raw);
            if (!custom.isEmpty()) {
                customIds.add(custom);
            }
            if (raw.indexOf(':') >= 0) {
                namespaced.add(raw);
            } else {
                simple.add(raw);
            }
        }
        this.normalizedConflictIds = customIds.isEmpty() ? Set.of() : Set.copyOf(customIds);
        this.conflictNamespacedKeys = namespaced.isEmpty() ? Set.of() : Set.copyOf(namespaced);
        this.conflictSimpleKeys = simple.isEmpty() ? Set.of() : Set.copyOf(simple);
    }

    /**
     * O(1) 判断是否与指定自定义附魔ID冲突 (入参须已按 normalizeCustomId 归一化)
     */
    public boolean conflictsWithCustom(String normalizedId) {
        return normalizedId != null && !normalizedId.isEmpty()
                && normalizedConflictIds.contains(normalizedId);
    }

    /**
     * O(1) 判断是否与指定原版附魔 key 冲突 (入参须为小写)
     */
    public boolean conflictsWithBukkitKey(String fullKey, String simpleKey) {
        return (fullKey != null && conflictNamespacedKeys.contains(fullKey))
                || (simpleKey != null && conflictSimpleKeys.contains(simpleKey));
    }

    public ObtainSettings getObtain() {
        return obtain;
    }

    public void setObtain(ObtainSettings obtain) {
        this.obtain = obtain == null ? new ObtainSettings() : obtain;
    }

    public Map<String, Integer> getCodexPools() {
        return codexPools;
    }

    public void setCodexPools(Map<String, Integer> codexPools) {
        this.codexPools = codexPools == null || codexPools.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new HashMap<>(codexPools));
    }

    public List<EffectBlock> getEffects() {
        return effects;
    }

    public void setEffects(List<EffectBlock> effects) {
        this.effects = effects == null || effects.isEmpty()
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(effects));
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // ==================== 内嵌类 ====================

    /**
     * 获取方式配置
     */
    public static class ObtainSettings {
        private boolean enchantingTable = true;
        private boolean anvil = true;
        private boolean villagerTrade = true;
        private int enchantingTableWeight = 10;
        private int[] villagerTradePriceRange = {10, 30};

        public boolean isEnchantingTable() {
            return enchantingTable;
        }

        public void setEnchantingTable(boolean enchantingTable) {
            this.enchantingTable = enchantingTable;
        }

        public boolean isAnvil() {
            return anvil;
        }

        public void setAnvil(boolean anvil) {
            this.anvil = anvil;
        }

        public boolean isVillagerTrade() {
            return villagerTrade;
        }

        public void setVillagerTrade(boolean villagerTrade) {
            this.villagerTrade = villagerTrade;
        }

        public int getEnchantingTableWeight() {
            return enchantingTableWeight;
        }

        public void setEnchantingTableWeight(int enchantingTableWeight) {
            this.enchantingTableWeight = enchantingTableWeight;
        }

        public int[] getVillagerTradePriceRange() {
            return villagerTradePriceRange;
        }

        public void setVillagerTradePriceRange(int[] villagerTradePriceRange) {
            this.villagerTradePriceRange = villagerTradePriceRange == null
                    ? new int[]{10, 30}
                    : villagerTradePriceRange;
        }
    }

    /**
     * 单个效果块配置（对应 YAML 中 effects 列表的一项）
     */
    public static class EffectBlock {
        private String trigger;
        private List<ConditionConfig> conditions = new ArrayList<>();
        private List<ActionConfig> actions = new ArrayList<>();
        private int cooldown;
        private Map<Integer, Integer> cooldownLevels = new HashMap<>();
        private String cooldownFormula;

        public String getTrigger() {
            return trigger;
        }

        public void setTrigger(String trigger) {
            this.trigger = trigger;
        }

        public List<ConditionConfig> getConditions() {
            return conditions;
        }

        public void setConditions(List<ConditionConfig> conditions) {
            this.conditions = conditions == null ? new ArrayList<>() : conditions;
        }

        public List<ActionConfig> getActions() {
            return actions;
        }

        public void setActions(List<ActionConfig> actions) {
            this.actions = actions == null ? new ArrayList<>() : actions;
        }

        public int getCooldown() {
            return cooldown;
        }

        public void setCooldown(int cooldown) {
            this.cooldown = cooldown;
        }

        public Map<Integer, Integer> getCooldownLevels() {
            return cooldownLevels;
        }

        public void setCooldownLevels(Map<Integer, Integer> cooldownLevels) {
            this.cooldownLevels = cooldownLevels == null ? new HashMap<>() : new HashMap<>(cooldownLevels);
        }

        public String getCooldownFormula() {
            return cooldownFormula;
        }

        public void setCooldownFormula(String cooldownFormula) {
            this.cooldownFormula = cooldownFormula;
        }
    }

    /**
     * 条件配置
     */
    public static class ConditionConfig {
        private String type;
        private String value;
        private Map<String, Object> extraParams = new HashMap<>();

        public ConditionConfig() {
        }

        public ConditionConfig(String type, String value) {
            this.type = type;
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public Map<String, Object> getExtraParams() {
            return extraParams;
        }

        public void setExtraParams(Map<String, Object> extraParams) {
            this.extraParams = extraParams == null ? new HashMap<>() : extraParams;
        }

        /**
         * 获取额外参数（带类型转换），未设置或类型不符时返回默认值。
         * 数字类型按默认值的具体类型转换, 兼容 YAML 把 3 读成 Integer 而调用方期望 Double 的情况。
         */
        @SuppressWarnings("unchecked")
        public <T> T getExtra(String key, T defaultValue) {
            Object v = extraParams.get(key);
            if (v == null) {
                return defaultValue;
            }
            if (defaultValue != null) {
                if (defaultValue instanceof Number && v instanceof Number number) {
                    Object converted = convertNumber(number, (Number) defaultValue);
                    if (converted != null) {
                        return (T) converted;
                    }
                }
                if (!defaultValue.getClass().isInstance(v)) {
                    return defaultValue;
                }
            }
            return (T) v;
        }

        /**
         * 获取不可修改的额外参数视图
         */
        public Map<String, Object> getExtraParamsView() {
            return Collections.unmodifiableMap(extraParams);
        }

        // ==================== 兼容性辅助方法（仿 ConfigurationSection API） ====================

        /**
         * 获取字符串。若 key 为 "value" 则返回主 value 字段；否则从 extraParams 读取。
         */
        public String getString(String key) {
            return getString(key, null);
        }

        /**
         * 获取字符串，未设置时返回默认值。
         */
        public String getString(String key, String defaultValue) {
            if ("value".equals(key)) {
                return value != null ? value : defaultValue;
            }
            Object v = extraParams.get(key);
            return v != null ? String.valueOf(v) : defaultValue;
        }

        /**
         * 获取布尔值，支持 Boolean 与可解析字符串。
         */
        public boolean getBoolean(String key, boolean defaultValue) {
            String s;
            if ("value".equals(key)) {
                s = value;
            } else {
                Object o = extraParams.get(key);
                if (o instanceof Boolean b) {
                    return b;
                }
                s = o == null ? null : String.valueOf(o);
            }
            if (s == null || s.isEmpty()) {
                return defaultValue;
            }
            if (s.equalsIgnoreCase("true")) {
                return true;
            }
            if (s.equalsIgnoreCase("false")) {
                return false;
            }
            return defaultValue;
        }

        /**
         * 获取字符串列表，支持 List 与单字符串兜底。
         * key 为 "value" 时优先读 extraParams, 缺失则回退主 value 字段。
         */
        public List<String> getStringList(String key) {
            Object v;
            if ("value".equals(key)) {
                v = extraParams.get("value");
                if (v == null && value != null && !value.isEmpty()) {
                    List<String> result = new ArrayList<>(1);
                    result.add(value);
                    return result;
                }
            } else {
                v = extraParams.get(key);
            }
            if (v instanceof List<?> list) {
                List<String> result = new ArrayList<>(list.size());
                for (Object item : list) {
                    if (item != null) {
                        result.add(String.valueOf(item));
                    }
                }
                return result;
            }
            if (v != null) {
                List<String> result = new ArrayList<>(1);
                result.add(String.valueOf(v));
                return result;
            }
            return new ArrayList<>();
        }
    }

    /**
     * 动作配置
     */
    public static class ActionConfig {
        private String type;
        private String value;
        private Map<String, Object> extraParams = new HashMap<>();

        public ActionConfig() {
        }

        public ActionConfig(String type, String value) {
            this.type = type;
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public Map<String, Object> getExtraParams() {
            return extraParams;
        }

        public void setExtraParams(Map<String, Object> extraParams) {
            this.extraParams = extraParams == null ? new HashMap<>() : extraParams;
        }

        /**
         * 获取额外参数（带类型转换），未设置或类型不符时返回默认值。
         * 数字类型按默认值的具体类型转换, 兼容 YAML 把 3 读成 Integer 而调用方期望 Double 的情况。
         */
        @SuppressWarnings("unchecked")
        public <T> T getExtra(String key, T defaultValue) {
            Object v = extraParams.get(key);
            if (v == null) {
                return defaultValue;
            }
            if (defaultValue != null) {
                if (defaultValue instanceof Number && v instanceof Number number) {
                    Object converted = convertNumber(number, (Number) defaultValue);
                    if (converted != null) {
                        return (T) converted;
                    }
                }
                if (!defaultValue.getClass().isInstance(v)) {
                    return defaultValue;
                }
            }
            return (T) v;
        }

        /**
         * 获取不可修改的额外参数视图
         */
        public Map<String, Object> getExtraParamsView() {
            return Collections.unmodifiableMap(extraParams);
        }
    }

    /**
     * 数字按目标默认值的具体类型转换; 目标类型不受支持时返回 null
     */
    private static Object convertNumber(Number value, Number target) {
        if (target instanceof Double) {
            return value.doubleValue();
        }
        if (target instanceof Integer) {
            return value.intValue();
        }
        if (target instanceof Long) {
            return value.longValue();
        }
        if (target instanceof Float) {
            return value.floatValue();
        }
        return null;
    }
}
