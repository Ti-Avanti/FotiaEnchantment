package gg.fotia.enchantment.item;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.util.ItemUtils;
import org.bukkit.NamespacedKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 星芒魔典逻辑
 * 带稀有度标记（dustlight/moonlit/radiant/aureate/divine）
 * 可通过消耗星辉残页合成随机品质魔典
 * 右键揭示获取随机附魔书
 */
public class StellarisCodex {

    private final FotiaEnchantment plugin;
    private final CustomItemManager itemManager;

    public StellarisCodex(FotiaEnchantment plugin, CustomItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    /**
     * 创建指定稀有度的星芒魔典
     *
     * @param player 玩家（多语言）
     * @param rarity 稀有度
     * @return 星芒魔典物品
     */
    public ItemStack create(Player player, String rarity) {
        return itemManager.createStellarisCodex(player, rarity);
    }

    /**
     * 揭示魔典 - 从EnchantmentManager.rollCodex(rarity)获取随机附魔，创建附魔书
     *
     * @param player 玩家
     * @param codex  魔典物品
     * @return 生成的附魔书，失败返回null
     */
    public ItemStack reveal(Player player, ItemStack codex) {
        String rarity = itemManager.getCodexRarity(codex);
        if (rarity == null) {
            return null;
        }

        EnchantmentManager enchantManager = plugin.getEnchantmentManager();
        String enchantId = enchantManager.rollCodex(rarity);
        if (enchantId == null) {
            return null;
        }

        EnchantmentData enchantData = enchantManager.getEnchantment(enchantId);
        if (enchantData == null) {
            return null;
        }

        // 随机等级（1 到 maxLevel）
        int maxLevel = enchantData.getMaxLevel();
        int level = maxLevel > 1 ? ThreadLocalRandom.current().nextInt(1, maxLevel + 1) : 1;

        // 创建附魔书
        ItemStack book = createEnchantedBook(player, enchantData, level);

        // 发送成功消息
        YamlConfiguration rarityConfig = plugin.getConfigManager().getRarityConfig();
        String rarityColor = rarityConfig.getString(rarity + ".color", "<white>");
        String enchantName = plugin.getLanguageManager().getEnchantName(player, enchantId);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("rarity_color", rarityColor);
        placeholders.put("enchant_name", enchantName);
        placeholders.put("level", toRoman(level));

        plugin.getMessageHelper().sendMessage(player, "codex-reveal-success", placeholders);

        return book;
    }

    /**
     * 合成魔典 - 消耗残页合成随机品质魔典
     *
     * @param player        玩家
     * @param fragmentCount 当前残页数量（已验证足够）
     * @return 合成的魔典，失败返回null
     */
    public ItemStack craft(Player player, int fragmentCount) {
        int cost = plugin.getConfigManager().getStellarisCodexFragmentCost();
        if (fragmentCount < cost) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("cost", String.valueOf(cost));
            plugin.getMessageHelper().sendMessage(player, "fragment-not-enough", placeholders);
            return null;
        }

        // 消耗残页
        StarweaveFragment fragment = itemManager.getStarweaveFragment();
        if (!fragment.consume(player, cost)) {
            return null;
        }

        String rarity = getCraftRarity();

        // 创建魔典
        ItemStack codex = create(player, rarity);

        // 发送成功消息
        YamlConfiguration rarityConfig = plugin.getConfigManager().getRarityConfig();
        String rarityColor = rarityConfig.getString(rarity + ".color", "<white>");
        String rarityName = itemManager.getRarityDisplayName(player, rarity);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("rarity_color", rarityColor);
        placeholders.put("rarity_name", rarityName);

        plugin.getMessageHelper().sendMessage(player, "codex-craft-success", placeholders);

        return codex;
    }

    /**
     * 按配置权重滚动品质，优先读取 config.yml 中的 stellaris-codex.rarity-weights。
     *
     * @return 随机品质名称
     */
    public String rollRarity() {
        Map<String, Integer> configuredWeights = plugin.getConfigManager().getStellarisCodexRarityWeights();
        List<String> rarities = new ArrayList<>(configuredWeights.keySet());
        List<Integer> weights = new ArrayList<>(configuredWeights.values());
        int totalWeight = 0;
        for (int weight : weights) {
            totalWeight += weight;
        }

        if (rarities.isEmpty() || totalWeight <= 0) {
            return "dustlight";
        }

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < rarities.size(); i++) {
            cumulative += weights.get(i);
            if (roll < cumulative) {
                return rarities.get(i);
            }
        }

        return rarities.get(rarities.size() - 1);
    }

    public String getCraftRarity() {
        String rarity = rollRarity();
        return rarity != null ? rarity : CodexCraftRarity.DEFAULT_RARITY;
    }

    public String getCraftPreviewRarity() {
        return CodexCraftRarity.DEFAULT_RARITY;
    }

    /**
     * 创建附魔书物品
     *
     * @param player  玩家
     * @param enchant 附魔数据
     * @param level   附魔等级
     * @return 附魔书
     */
    public ItemStack createEnchantedBook(Player player, EnchantmentData enchant, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);

        // 通过PDC存储附魔数据
        plugin.getEnchantmentManager().getPdcManager().addEnchantment(book, enchant.getId(), level);

        // 设置显示名称和lore
        String enchantName = plugin.getLanguageManager().getEnchantName(player, enchant.getId());
        YamlConfiguration rarityConfig = plugin.getConfigManager().getRarityConfig();
        String rarityColor = rarityConfig.getString(enchant.getRarity() + ".color", "<white>");

        String displayName = "<!i>" + rarityColor + enchantName + " " + toRoman(level);
        Component nameComp = itemManager.parseWithPlaceholders(displayName, new HashMap<>());

        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.displayName(nameComp);
            meta.setEnchantmentGlintOverride(true);
            applyBookAppearance(meta, enchant.getRarity());
            book.setItemMeta(meta);
        }

        return book;
    }

    private void applyBookAppearance(ItemMeta meta, String rarity) {
        EnchantmentBookAppearance appearance = EnchantmentBookAppearance.from(
                plugin.getConfigManager().getEnchantmentBooksConfig(),
                rarity
        );
        if (appearance.customModelData() != null) {
            meta.setCustomModelData(appearance.customModelData());
        }
        applyNamespacedKey(appearance.itemModel(), key -> ItemUtils.setItemModel(meta, key), "item-model");
        applyNamespacedKey(appearance.tooltipStyle(), key -> ItemUtils.setTooltipStyle(meta, key), "tooltip-style");
    }

    private void applyNamespacedKey(String raw, java.util.function.Consumer<NamespacedKey> setter, String fieldName) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            NamespacedKey key = NamespacedKey.fromString(raw, plugin);
            if (key == null) {
                plugin.getLogger().warning("无效的自定义附魔书 " + fieldName + ": " + raw);
                return;
            }
            setter.accept(key);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("无效的自定义附魔书 " + fieldName + ": " + raw);
        }
    }

    /**
     * 检查物品是否为星芒魔典
     *
     * @param item 物品
     * @return 是否为星芒魔典
     */
    public boolean isStellarisCodex(ItemStack item) {
        return "stellaris-codex".equals(itemManager.identifyItem(item));
    }

    /**
     * 数字转罗马数字
     */
    private String toRoman(int num) {
        if (num <= 0) return String.valueOf(num);
        String[] thousands = {"", "M", "MM", "MMM"};
        String[] hundreds = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] tens = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] ones = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

        if (num <= 10) return ones[num];
        if (num > 3999) return String.valueOf(num);

        return thousands[num / 1000] +
                hundreds[(num % 1000) / 100] +
                tens[(num % 100) / 10] +
                ones[num % 10];
    }
}
