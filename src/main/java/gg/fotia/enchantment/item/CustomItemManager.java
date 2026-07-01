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

    public void init() {
        starweaveFragment = new StarweaveFragment(plugin, this);
        stellarisCodex = new StellarisCodex(plugin, this);
        disenchantStone = new DisenchantStone(plugin, this);
        plugin.getLogger().info("Custom item manager initialized.");
    }

    public ItemStack createStarweaveFragment(Player player, int amount) {
        YamlConfiguration itemsConfig = plugin.getConfigManager().getItemsConfig();
        ConfigurationSection section = itemsConfig.getConfigurationSection("starweave-fragment");
        if (section == null) {
            return new ItemStack(Material.AMETHYST_SHARD, amount);
        }

        String craftEngineItem = section.getString("craftengine-item", "");
        if (!craftEngineItem.isEmpty()) {
            ItemStack ceItem = createFromCraftEngine(craftEngineItem, amount, player);
            if (ceItem != null) {
                setItemType(ceItem, "starweave-fragment");
                return ceItem;
            }
        }

        Material material = Material.matchMaterial(section.getString("material", "AMETHYST_SHARD"));
        if (material == null) {
            material = Material.AMETHYST_SHARD;
        }
        CustomItemAppearance appearance = CustomItemAppearance.from(section);
        int customModelData = appearance.customModelData() != null ? appearance.customModelData() : 0;
        boolean glow = section.getBoolean("glow", true);

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
        setItemType(item, "starweave-fragment");
        return item;
    }

    public ItemStack createStellarisCodex(Player player, String rarity) {
        YamlConfiguration itemsConfig = plugin.getConfigManager().getItemsConfig();
        ConfigurationSection section = itemsConfig.getConfigurationSection("stellaris-codex." + rarity);
        if (section == null) {
            return new ItemStack(Material.ENCHANTED_BOOK);
        }

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
        if (material == null) {
            material = Material.ENCHANTED_BOOK;
        }
        CustomItemAppearance appearance = CustomItemAppearance.from(section);
        int customModelData = appearance.customModelData() != null ? appearance.customModelData() : 0;
        boolean glow = section.getBoolean("glow", true);

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
        setItemType(item, "stellaris-codex");
        setItemRarity(item, rarity);
        return item;
    }

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
        if (material == null) {
            material = Material.ENCHANTED_BOOK;
        }

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

    public ItemStack createDisenchantStone(Player player, String key) {
        YamlConfiguration itemsConfig = plugin.getConfigManager().getItemsConfig();
        String configKey = key;
        ConfigurationSection section = DisenchantItemRegistry.sectionForConfigKey(itemsConfig, configKey);
        if (section == null) {
            configKey = DisenchantItemRegistry.configKey(itemsConfig, key);
            section = DisenchantItemRegistry.sectionForConfigKey(itemsConfig, configKey);
        }
        if (section == null) {
            return new ItemStack(Material.PRISMARINE_SHARD);
        }

        String itemId = tierToItemId(configKey);
        String craftEngineItem = section.getString("craftengine-item", "");
        if (!craftEngineItem.isEmpty()) {
            ItemStack ceItem = createFromCraftEngine(craftEngineItem, 1, player);
            if (ceItem != null) {
                setItemType(ceItem, itemId);
                return ceItem;
            }
        }

        Material material = Material.matchMaterial(section.getString("material", "PRISMARINE_SHARD"));
        if (material == null) {
            material = Material.PRISMARINE_SHARD;
        }
        CustomItemAppearance appearance = CustomItemAppearance.from(section);
        int customModelData = appearance.customModelData() != null ? appearance.customModelData() : 0;
        boolean glow = section.getBoolean("glow", false);

        Map<String, String> placeholders = disenchantItemPlaceholders(player, section);
        String nameStr = configuredItemName(player, itemId, section);
        List<String> loreStrList = configuredItemLore(player, itemId, section);

        Component name = parseWithPlaceholders(nameStr, placeholders);
        List<Component> lore = parseLoreWithPlaceholders(loreStrList, placeholders);

        ItemStack item = ItemUtils.buildItem(material, name, lore, customModelData, glow);
        applyAppearance(item, appearance);
        setItemType(item, itemId);
        return item;
    }

    public ItemStack createAnvilBreakthroughStone(Player player, int amount) {
        YamlConfiguration itemsConfig = plugin.getConfigManager().getItemsConfig();
        ConfigurationSection section = itemsConfig.getConfigurationSection("anvil-breakthrough-stone");
        if (section == null) {
            return new ItemStack(Material.ECHO_SHARD, amount);
        }

        String craftEngineItem = section.getString("craftengine-item", "");
        if (!craftEngineItem.isEmpty()) {
            ItemStack ceItem = createFromCraftEngine(craftEngineItem, amount, player);
            if (ceItem != null) {
                setItemType(ceItem, "anvil-breakthrough-stone");
                return ceItem;
            }
        }

        Material material = Material.matchMaterial(section.getString("material", "ECHO_SHARD"));
        if (material == null) {
            material = Material.ECHO_SHARD;
        }
        CustomItemAppearance appearance = CustomItemAppearance.from(section);
        int customModelData = appearance.customModelData() != null ? appearance.customModelData() : 0;
        boolean glow = section.getBoolean("glow", true);

        String nameStr = plugin.getLanguageManager().getItemName(player, "anvil-breakthrough-stone");
        List<String> loreStrList = plugin.getLanguageManager().getItemLore(player, "anvil-breakthrough-stone");

        Component name = parseWithPlaceholders(nameStr, new HashMap<>());
        List<Component> lore = parseLoreWithPlaceholders(loreStrList, new HashMap<>());

        ItemStack item = ItemUtils.buildItem(material, name, lore, customModelData, glow);
        item.setAmount(amount);
        applyAppearance(item, appearance);
        setItemType(item, "anvil-breakthrough-stone");
        return item;
    }

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

    public boolean isAnvilBreakthroughStone(ItemStack item) {
        return "anvil-breakthrough-stone".equals(identifyItem(item));
    }

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

    public void setItemType(ItemStack item, String type) {
        if (item == null || type == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(itemTypeKey, PersistentDataType.STRING, type);
        item.setItemMeta(meta);
    }

    public void setItemRarity(ItemStack item, String rarity) {
        if (item == null || rarity == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(itemRarityKey, PersistentDataType.STRING, rarity);
        item.setItemMeta(meta);
    }

    public Component parseWithPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }
        text = LegacyColorConverter.convert(text);
        return miniMessage.deserialize(text);
    }

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

    public String getRarityDisplayName(Player player, String rarity) {
        String key = "rarity-" + rarity;
        return plugin.getLanguageManager().getMessage(player, key);
    }

    public String tierToItemId(String tier) {
        String itemId = DisenchantItemRegistry.itemIdForConfigKey(plugin.getConfigManager().getItemsConfig(), tier);
        return itemId != null ? itemId : "disenchant-shard";
    }

    public String itemIdToTier(String itemId) {
        return DisenchantItemRegistry.configKey(plugin.getConfigManager().getItemsConfig(), itemId);
    }

    public boolean isDisenchantItemType(String itemId) {
        return DisenchantItemRegistry.isDisenchantItem(plugin.getConfigManager().getItemsConfig(), itemId);
    }

    public List<String> getDisenchantItemTypes() {
        return new ArrayList<>(DisenchantItemRegistry.itemIds(plugin.getConfigManager().getItemsConfig()));
    }

    public String getDisenchantItemDisplayName(Player player, String itemId) {
        ConfigurationSection section = DisenchantItemRegistry.section(plugin.getConfigManager().getItemsConfig(), itemId);
        return configuredItemName(player, itemId, section);
    }

    private Map<String, String> disenchantItemPlaceholders(Player player, ConfigurationSection section) {
        Map<String, String> placeholders = new HashMap<>();
        DisenchantSource source = DisenchantItemRegistry.source(section);
        placeholders.put("source", sourceDisplayName(player, source));
        placeholders.put("source_id", source.name());
        placeholders.put("success_chance", String.valueOf(section.getInt("success-chance", 80)));
        placeholders.put("max_remove", String.valueOf(section.getInt("max-remove", 1)));
        placeholders.put("max_rarity", section.getString("max-rarity", "divine"));
        placeholders.put("selectable", String.valueOf(section.getBoolean("selectable", false)));
        return placeholders;
    }

    private String sourceDisplayName(Player player, DisenchantSource source) {
        return switch (source) {
            case VANILLA -> plugin.getLanguageManager().getGUIText(player, "disenchant-gui.source-vanilla");
            case FOTIA -> plugin.getLanguageManager().getGUIText(player, "disenchant-gui.source-fotia");
            case ANY -> plugin.getLanguageManager().getGUIText(player, "disenchant-gui.source-any");
        };
    }

    private String configuredItemName(Player player, String itemId, ConfigurationSection section) {
        String name = plugin.getLanguageManager().getItemName(player, itemId);
        if ((name == null || name.equals(itemId)) && section != null && section.isString("name")) {
            return section.getString("name", itemId);
        }
        return name;
    }

    private List<String> configuredItemLore(Player player, String itemId, ConfigurationSection section) {
        List<String> lore = plugin.getLanguageManager().getItemLore(player, itemId);
        if (lore.isEmpty() && section != null && section.isList("lore")) {
            return section.getStringList("lore");
        }
        return lore;
    }

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
