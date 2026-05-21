package gg.fotia.enchantment.item;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.integration.CraftEngineHook;
import gg.fotia.enchantment.util.ItemUtils;
import gg.fotia.enchantment.util.LegacyColorConverter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义道具管理器
 * 管理所有自定义道具的创建和识别
 */
public class CustomItemManager {

    private final FotiaEnchantment plugin;
    private final NamespacedKey itemTypeKey;
    private final NamespacedKey itemRarityKey;
    private final MiniMessage miniMessage;

    private StarweaveFragment starweaveFragment;
    private StellarisCodex stellarisCodex;
    private DisenchantStone disenchantStone;

    public CustomItemManager(FotiaEnchantment plugin) {
        this.plugin = plugin;
        this.itemTypeKey = new NamespacedKey("fotia", "item_type");
        this.itemRarityKey = new NamespacedKey("fotia", "item_rarity");
        this.miniMessage = MiniMessage.miniMessage();
    }

    /**
     * 初始化道具管理器
     */
    public void init() {
        starweaveFragment = new StarweaveFragment(plugin, this);
        stellarisCodex = new StellarisCodex(plugin, this);
        disenchantStone = new DisenchantStone(plugin, this);
        plugin.getLogger().info("自定义道具管理器已初始化");
    }

    // ==================== 创建方法 ====================

    /**
     * 创建星辉残页物品
     *
     * @param player 玩家（用于多语言）
     * @param amount 数量
     * @return 创建的物品
     */
    public ItemStack createStarweaveFragment(Player player, int amount) {
        YamlConfiguration itemsConfig = plugin.getConfigManager().getItemsConfig();
        ConfigurationSection section = itemsConfig.getConfigurationSection("starweave-fragment");
        if (section == null) {
            return new ItemStack(Material.AMETHYST_SHARD, amount);
        }

        // 检查CraftEngine
        String craftEngineItem = section.getString("craftengine-item", "");
        if (!craftEngineItem.isEmpty()) {
            ItemStack ceItem = createFromCraftEngine(craftEngineItem, amount, player);
            if (ceItem != null) {
                setItemType(ceItem, "starweave-fragment");
                return ceItem;
            }
        }

        Material material = Material.matchMaterial(section.getString("material", "AMETHYST_SHARD"));
        if (material == null) material = Material.AMETHYST_SHARD;
        CustomItemAppearance appearance = CustomItemAppearance.from(section);
        int customModelData = appearance.customModelData() != null ? appearance.customModelData() : 0;
        boolean glow = section.getBoolean("glow", true);

        // 获取名称和lore
        int fragmentCost = plugin.getConfigManager().getStellarisCodexFragmentCost();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("cost", String.valueOf(fragmentCost));

        String nameStr = plugin.getLanguageManager().getItemName(player, "starweave-fragment");
        List<String> loreStrList = plugin.getLanguageManager().getItemLore(player, "starweave-fragment");

        Component name = parseWithPlaceholders(nameStr, placeholders);
        List<Component> lore = parseLoreWithPlaceholders(loreStrList, placeholders);

        ItemStack item = ItemUtils.buildItem(material, name, lore, customModelData, glow);
        item.setAmount(amount);

        applyAppearance(item, appearance);

        // 设置PDC标记
        setItemType(item, "starweave-fragment");

        return item;
    }

    /**
     * 创建指定稀有度的星芒魔典
     *
     * @param player 玩家（用于多语言）
     * @param rarity 稀有度
     * @return 创建的物品
     */
    public ItemStack createStellarisCodex(Player player, String rarity) {
        YamlConfiguration itemsConfig = plugin.getConfigManager().getItemsConfig();
        ConfigurationSection section = itemsConfig.getConfigurationSection("stellaris-codex." + rarity);
        if (section == null) {
            return new ItemStack(Material.ENCHANTED_BOOK);
        }

        // 检查CraftEngine
        String craftEngineItem = section.getString("craftengine-item", "");
        if (!craftEngineItem.isEmpty()) {
            ItemStack ceItem = createFromCraftEngine(craftEngineItem, 1, player);
            if (ceItem != null) {
                setItemType(ceItem, "stellaris-codex");
                setItemRarity(ceItem, rarity);
                return ceItem;
            }
        }

        Material material = Material.matchMaterial(section.getString("material", "ENCHANTED_BOOK"));
        if (material == null) material = Material.ENCHANTED_BOOK;
        CustomItemAppearance appearance = CustomItemAppearance.from(section);
        int customModelData = appearance.customModelData() != null ? appearance.customModelData() : 0;
        boolean glow = section.getBoolean("glow", true);

        // 获取稀有度相关信息
        YamlConfiguration rarityConfig = plugin.getConfigManager().getRarityConfig();
        String rarityColor = rarityConfig.getString(rarity + ".color", "<white>");
        String rarityName = getRarityDisplayName(player, rarity);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("rarity_name", rarityName);
        placeholders.put("rarity_color", rarityColor);

        String nameStr = plugin.getLanguageManager().getItemName(player, "stellaris-codex");
        List<String> loreStrList = plugin.getLanguageManager().getItemLore(player, "stellaris-codex");

        Component name = parseWithPlaceholders(nameStr, placeholders);
        List<Component> lore = parseLoreWithPlaceholders(loreStrList, placeholders);

        ItemStack item = ItemUtils.buildItem(material, name, lore, customModelData, glow);

        applyAppearance(item, appearance);

        // 设置PDC标记
        setItemType(item, "stellaris-codex");
        setItemRarity(item, rarity);

        return item;
    }

    /**
     * 创建碎片合成 GUI 中使用的随机品质预览物品，不写入品质标记。
     */
    public ItemStack createStellarisCodexPreview(Player player) {
        YamlConfiguration itemsConfig = plugin.getConfigManager().getItemsConfig();
        ConfigurationSection section = itemsConfig.getConfigurationSection("stellaris-codex.preview");

        String craftEngineItem = section != null ? section.getString("craftengine-item", "") : "";
        if (craftEngineItem != null && !craftEngineItem.isEmpty()) {
            ItemStack ceItem = createFromCraftEngine(craftEngineItem, 1, player);
            if (ceItem != null) {
                return ceItem;
            }
        }

        String materialName = section != null ? section.getString("material", "ENCHANTED_BOOK") : "ENCHANTED_BOOK";
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.ENCHANTED_BOOK;

        CustomItemAppearance appearance = CustomItemAppearance.from(section);
        int customModelData = appearance.customModelData() != null ? appearance.customModelData() : 0;
        boolean glow = section == null || section.getBoolean("glow", true);

        String nameStr = plugin.getLanguageManager().getItemName(player, "stellaris-codex-preview");
        List<String> loreStrList = plugin.getLanguageManager().getItemLore(player, "stellaris-codex-preview");

        Component name = parseWithPlaceholders(nameStr, new HashMap<>());
        List<Component> lore = parseLoreWithPlaceholders(loreStrList, new HashMap<>());

        ItemStack item = ItemUtils.buildItem(material, name, lore, customModelData, glow);
        applyAppearance(item, appearance);
        return item;
    }

    /**
     * 创建祛魔之石
     *
     * @param player 玩家（用于多语言）
     * @param tier   等级（tier-1/tier-2/tier-3）
     * @return 创建的物品
     */
    public ItemStack createDisenchantStone(Player player, String tier) {
        YamlConfiguration itemsConfig = plugin.getConfigManager().getItemsConfig();
        ConfigurationSection section = itemsConfig.getConfigurationSection("disenchant-stone.tiers." + tier);
        if (section == null) {
            return new ItemStack(Material.PRISMARINE_SHARD);
        }

        Material material = Material.matchMaterial(section.getString("material", "PRISMARINE_SHARD"));
        if (material == null) material = Material.PRISMARINE_SHARD;
        int customModelData = section.getInt("custom-model-data", 0);
        boolean glow = section.getBoolean("glow", false);

        // 获取对应语言键
        String itemId = tierToItemId(tier);
        String nameStr = plugin.getLanguageManager().getItemName(player, itemId);
        List<String> loreStrList = plugin.getLanguageManager().getItemLore(player, itemId);

        Component name = parseWithPlaceholders(nameStr, new HashMap<>());
        List<Component> lore = parseLoreWithPlaceholders(loreStrList, new HashMap<>());

        ItemStack item = ItemUtils.buildItem(material, name, lore, customModelData, glow);

        // 设置PDC标记
        setItemType(item, itemId);

        return item;
    }

    // ==================== 识别方法 ====================

    /**
     * 识别物品类型
     *
     * @param item 物品
     * @return 物品类型ID，如 "starweave-fragment"、"stellaris-codex"、
     *         "disenchant-shard"、"disenchant-crystal"、"disenchant-gem"，无法识别返回 null
     */
    public String identifyItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(itemTypeKey, PersistentDataType.STRING);
    }

    /**
     * 获取魔典的稀有度
     *
     * @param item 物品
     * @return 稀有度名称，非魔典返回 null
     */
    public String getCodexRarity(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(itemRarityKey, PersistentDataType.STRING);
    }

    // ==================== PDC 操作 ====================

    /**
     * 设置物品类型标记
     */
    public void setItemType(ItemStack item, String type) {
        if (item == null || type == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(itemTypeKey, PersistentDataType.STRING, type);
        item.setItemMeta(meta);
    }

    /**
     * 设置物品稀有度标记
     */
    public void setItemRarity(ItemStack item, String rarity) {
        if (item == null || rarity == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(itemRarityKey, PersistentDataType.STRING, rarity);
        item.setItemMeta(meta);
    }

    // ==================== 工具方法 ====================

    /**
     * 解析带占位符的文本为 Component
     */
    public Component parseWithPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        // 替换占位符
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        // 转换旧颜色码
        text = LegacyColorConverter.convert(text);
        return miniMessage.deserialize(text);
    }

    /**
     * 解析带占位符的lore列表
     */
    public List<Component> parseLoreWithPlaceholders(List<String> loreList, Map<String, String> placeholders) {
        List<Component> components = new ArrayList<>();
        if (loreList == null || loreList.isEmpty()) {
            return components;
        }
        for (String line : loreList) {
            if (line == null || line.isEmpty()) {
                components.add(Component.empty());
            } else {
                components.add(parseWithPlaceholders(line, placeholders));
            }
        }
        return components;
    }

    private void applyAppearance(ItemStack item, CustomItemAppearance appearance) {
        if (appearance == null) {
            return;
        }
        if (appearance.itemModel() != null) {
            ItemUtils.setItemModel(item, appearance.itemModel());
        }
        if (appearance.tooltipStyle() != null) {
            ItemUtils.setTooltipStyle(item, appearance.tooltipStyle());
        }
    }

    /**
     * 获取稀有度显示名称（多语言）
     */
    public String getRarityDisplayName(Player player, String rarity) {
        String key = "rarity-" + rarity;
        return plugin.getLanguageManager().getMessage(player, key);
    }

    /**
     * tier名称转换为语言键对应的item id
     */
    public String tierToItemId(String tier) {
        return switch (tier) {
            case "tier-1" -> "disenchant-shard";
            case "tier-2" -> "disenchant-crystal";
            case "tier-3" -> "disenchant-gem";
            default -> "disenchant-shard";
        };
    }

    /**
     * item id 转换为 tier 名称
     */
    public String itemIdToTier(String itemId) {
        return switch (itemId) {
            case "disenchant-shard" -> "tier-1";
            case "disenchant-crystal" -> "tier-2";
            case "disenchant-gem" -> "tier-3";
            default -> null;
        };
    }

    /**
     * 通过 CraftEngine 创建物品
     *
     * @param itemId CraftEngine 物品ID
     * @param amount 数量
     * @param player 玩家上下文
     * @return 物品，失败返回 null
     */
    private ItemStack createFromCraftEngine(String itemId, int amount, Player player) {
        if (plugin.getIntegrationManager() == null) {
            return null;
        }
        CraftEngineHook hook = plugin.getIntegrationManager().getCraftEngineHook();
        if (hook == null || !hook.isAvailable()) {
            return null;
        }
        return hook.createItem(itemId, amount, player);
    }

    // ==================== Getter ====================

    public NamespacedKey getItemTypeKey() {
        return itemTypeKey;
    }

    public NamespacedKey getItemRarityKey() {
        return itemRarityKey;
    }

    public StarweaveFragment getStarweaveFragment() {
        return starweaveFragment;
    }

    public StellarisCodex getStellarisCodex() {
        return stellarisCodex;
    }

    public DisenchantStone getDisenchantStone() {
        return disenchantStone;
    }
}
