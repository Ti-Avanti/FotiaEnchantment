package gg.fotia.enchantment.item;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.lore.EnchantmentLoreCleaner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 祛魔之石逻辑
 * 三个等级:
 * - tier-1(碎石): 随机拆卸1个, 成功率从配置读取, 最高辉芒(radiant)
 * - tier-2(晶石): 可选择拆卸最多3个, 成功率从配置读取, 最高耀金(aureate)
 * - tier-3(神石): 可选择拆卸最多5个, 成功率从配置读取, 全等级
 */
public class DisenchantStone {

    /** 稀有度排序（从低到高） */
    private static final List<String> RARITY_ORDER = Arrays.asList(
            "dustlight", "moonlit", "radiant", "aureate", "divine"
    );

    private final FotiaEnchantment plugin;
    private final CustomItemManager itemManager;

    public DisenchantStone(FotiaEnchantment plugin, CustomItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    /**
     * 创建祛魔之石
     *
     * @param player 玩家（多语言）
     * @param tier   等级（tier-1/tier-2/tier-3）
     * @return 祛魔之石物品
     */
    public ItemStack create(Player player, String tier) {
        return itemManager.createDisenchantStone(player, tier);
    }

    /**
     * 执行拆卸操作
     *
     * @param player          玩家
     * @param equipment       装备物品
     * @param stone           祛魔之石
     * @param selectedEnchants 选择的附魔ID列表（tier-1时为null，系统随机选择）
     * @return 生成的附魔书列表，失败返回空列表
     */
    public List<ItemStack> disenchant(Player player, ItemStack equipment, ItemStack stone, List<String> selectedEnchants) {
        String itemId = itemManager.identifyItem(stone);
        if (itemId == null) {
            return Collections.emptyList();
        }
        String tier = itemManager.itemIdToTier(itemId);
        if (tier == null) {
            return Collections.emptyList();
        }

        ConfigurationSection tierConfig = getTierConfig(tier);
        if (tierConfig == null) {
            return Collections.emptyList();
        }

        PDCManager pdcManager = plugin.getEnchantmentManager().getPdcManager();
        Map<String, Integer> enchants = pdcManager.getEnchantments(equipment);
        if (enchants.isEmpty()) {
            return Collections.emptyList();
        }

        int maxRemove = tierConfig.getInt("max-remove", 1);
        boolean selectable = tierConfig.getBoolean("selectable", false);
        int successChance = tierConfig.getInt("success-chance", 80);
        boolean destroyOnFail = tierConfig.getBoolean("destroy-on-fail", false);
        boolean keepLevel = tierConfig.getBoolean("keep-level", true);

        // 确定要拆卸的附魔
        List<String> toRemove;
        if (!selectable || selectedEnchants == null || selectedEnchants.isEmpty()) {
            // tier-1: 随机选择
            List<String> available = new ArrayList<>(enchants.keySet());
            // 过滤稀有度限制
            available.removeIf(id -> !canDisenchant(tier, id));
            if (available.isEmpty()) {
                return Collections.emptyList();
            }
            Collections.shuffle(available);
            toRemove = available.subList(0, Math.min(maxRemove, available.size()));
        } else {
            // tier-2/tier-3: 使用玩家选择的
            toRemove = new ArrayList<>(selectedEnchants);
            // 限制数量
            if (toRemove.size() > maxRemove) {
                toRemove = toRemove.subList(0, maxRemove);
            }
            // 验证是否可以拆卸
            toRemove.removeIf(id -> !canDisenchant(tier, id) || !enchants.containsKey(id));
        }

        if (toRemove.isEmpty()) {
            return Collections.emptyList();
        }

        EnchantmentLoreCleaner.stripGeneratedLore(plugin, player, equipment);

        List<ItemStack> results = new ArrayList<>();

        for (String enchantId : toRemove) {
            int level = enchants.getOrDefault(enchantId, 1);

            // 判断成功率
            boolean success = ThreadLocalRandom.current().nextInt(100) < successChance;

            if (success) {
                // 成功：从装备移除附魔
                pdcManager.removeEnchantment(equipment, enchantId);

                // 生成附魔书
                int bookLevel = keepLevel ? level : 1;
                EnchantmentData enchantData = plugin.getEnchantmentManager().getEnchantment(enchantId);
                if (enchantData != null) {
                    ItemStack book = itemManager.getStellarisCodex().createEnchantedBook(player, enchantData, bookLevel);
                    results.add(book);
                }

                // 发送成功消息
                String enchantName = plugin.getLanguageManager().getEnchantName(player, enchantId);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("enchant_name", enchantName);
                placeholders.put("level", toRoman(level));
                plugin.getMessageHelper().sendMessage(player, "disenchant-success", placeholders);
            } else {
                // 失败
                if (destroyOnFail) {
                    // 附魔被销毁
                    pdcManager.removeEnchantment(equipment, enchantId);
                    plugin.getMessageHelper().sendMessage(player, "disenchant-destroyed");
                } else {
                    plugin.getMessageHelper().sendMessage(player, "disenchant-fail");
                }
            }
        }

        return results;
    }

    /**
     * 检查该等级石头是否能拆卸指定附魔（根据稀有度限制）
     *
     * @param tier     等级（tier-1/tier-2/tier-3）
     * @param enchantId 附魔ID
     * @return 是否可以拆卸
     */
    public boolean canDisenchant(String tier, String enchantId) {
        ConfigurationSection tierConfig = getTierConfig(tier);
        if (tierConfig == null) {
            return false;
        }

        String maxRarity = tierConfig.getString("max-rarity", "divine");
        EnchantmentData enchantData = plugin.getEnchantmentManager().getEnchantment(enchantId);
        if (enchantData == null) {
            // 未知附魔默认允许（可能是原版附魔）
            boolean allowVanilla = tierConfig.getBoolean("allow-vanilla", true);
            return allowVanilla;
        }

        String enchantRarity = enchantData.getRarity();
        if (enchantRarity == null) {
            return true;
        }

        // 比较稀有度等级
        int enchantRarityIndex = RARITY_ORDER.indexOf(enchantRarity.toLowerCase(Locale.ROOT));
        int maxRarityIndex = RARITY_ORDER.indexOf(maxRarity.toLowerCase(Locale.ROOT));

        if (enchantRarityIndex == -1 || maxRarityIndex == -1) {
            return true;
        }

        return enchantRarityIndex <= maxRarityIndex;
    }

    /**
     * 获取最大拆卸数量
     *
     * @param tier 等级
     * @return 最大拆卸数量
     */
    public int getMaxRemoveCount(String tier) {
        ConfigurationSection tierConfig = getTierConfig(tier);
        if (tierConfig == null) return 1;
        return tierConfig.getInt("max-remove", 1);
    }

    /**
     * 获取成功率
     *
     * @param tier 等级
     * @return 成功率（百分比 0-100）
     */
    public int getSuccessRate(String tier) {
        ConfigurationSection tierConfig = getTierConfig(tier);
        if (tierConfig == null) return 80;
        return tierConfig.getInt("success-chance", 80);
    }

    /**
     * 是否可以选择附魔（shard不可以，crystal和gem可以）
     *
     * @param tier 等级
     * @return 是否可选择
     */
    public boolean canSelect(String tier) {
        ConfigurationSection tierConfig = getTierConfig(tier);
        if (tierConfig == null) return false;
        return tierConfig.getBoolean("selectable", false);
    }

    /**
     * 检查物品是否为祛魔之石
     *
     * @param item 物品
     * @return 是否为祛魔之石
     */
    public boolean isDisenchantStone(ItemStack item) {
        String type = itemManager.identifyItem(item);
        if (type == null) return false;
        return type.startsWith("disenchant-");
    }

    /**
     * 获取祛魔之石的等级
     *
     * @param item 物品
     * @return 等级（tier-1/tier-2/tier-3），非祛魔之石返回null
     */
    public String getStoneTier(ItemStack item) {
        String type = itemManager.identifyItem(item);
        if (type == null) {
            return null;
        }
        return itemManager.itemIdToTier(type);
    }

    // ==================== 内部方法 ====================

    /**
     * 获取指定等级的配置节
     */
    private ConfigurationSection getTierConfig(String tier) {
        YamlConfiguration itemsConfig = plugin.getConfigManager().getItemsConfig();
        return itemsConfig.getConfigurationSection("disenchant-stone.tiers." + tier);
    }

    /**
     * 数字转罗马数字
     */
    private String toRoman(int num) {
        if (num <= 0) return String.valueOf(num);
        String[] ones = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (num <= 10) return ones[num];
        return String.valueOf(num);
    }
}
