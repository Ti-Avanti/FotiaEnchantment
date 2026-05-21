package gg.fotia.enchantment.core;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 附魔数据模型 - 存储一个附魔的完整配置信息
 *
 * <p>该对象由 YAML 配置加载器从单个附魔配置文件解析得到，
 * 表示一种自定义附魔的全部静态数据，包括基础属性、获取方式以及效果管道配置。
 */
public class EnchantmentData {

    /** 附魔ID（通常为文件名，不含扩展名） */
    private String id;

    /** 是否启用该附魔 */
    private boolean enabled = true;

    private boolean curse;

    /** 最大等级 */
    private int maxLevel = 1;

    /** 稀有度（引用 rarity.yml 中的 key） */
    private String rarity;

    /** 所属附魔组（引用 groups.yml 中的 key） */
    private String group;

    /** 适用物品类型 */
    private List<Material> applicableItems = new ArrayList<>();

    /** 冲突附魔ID列表 */
    private List<String> conflicts = new ArrayList<>();

    /** 获取方式配置 */
    private ObtainSettings obtain = new ObtainSettings();

    /** 星芒魔典抽取池权重 {稀有度: 权重} */
    private Map<String, Integer> codexPools = new HashMap<>();

    /** 效果管道配置列表 */
    private List<EffectBlock> effects = new ArrayList<>();

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
        this.applicableItems = applicableItems == null ? new ArrayList<>() : applicableItems;
    }

    public List<String> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<String> conflicts) {
        this.conflicts = conflicts == null ? new ArrayList<>() : conflicts;
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
        this.codexPools = codexPools == null ? new HashMap<>() : codexPools;
    }

    public List<EffectBlock> getEffects() {
        return effects;
    }

    public void setEffects(List<EffectBlock> effects) {
        this.effects = effects == null ? new ArrayList<>() : effects;
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
         * 获取额外参数（带类型转换），未设置时返回默认值
         */
        @SuppressWarnings("unchecked")
        public <T> T getExtra(String key, T defaultValue) {
            Object v = extraParams.get(key);
            if (v == null) {
                return defaultValue;
            }
            try {
                return (T) v;
            } catch (ClassCastException e) {
                return defaultValue;
            }
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
         */
        @SuppressWarnings("unchecked")
        public List<String> getStringList(String key) {
            Object v;
            if ("value".equals(key)) {
                v = extraParams.containsKey("value") ? extraParams.get("value") : null;
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
         * 获取额外参数（带类型转换），未设置时返回默认值
         */
        @SuppressWarnings("unchecked")
        public <T> T getExtra(String key, T defaultValue) {
            Object v = extraParams.get(key);
            if (v == null) {
                return defaultValue;
            }
            try {
                return (T) v;
            } catch (ClassCastException e) {
                return defaultValue;
            }
        }

        /**
         * 获取不可修改的额外参数视图
         */
        public Map<String, Object> getExtraParamsView() {
            return Collections.unmodifiableMap(extraParams);
        }
    }
}
