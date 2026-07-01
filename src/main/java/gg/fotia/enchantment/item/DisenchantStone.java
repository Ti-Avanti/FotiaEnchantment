package gg.fotia.enchantment.item;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.compat.BukkitRegistryCompat;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.lore.item.EnchantmentLoreCleaner;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class DisenchantStone {

    private static final List<String> RARITY_ORDER = Arrays.asList(
            "dustlight", "moonlit", "radiant", "aureate", "divine"
    );

    private final FotiaEnchantment plugin;
    private final CustomItemManager itemManager;

    public DisenchantStone(FotiaEnchantment plugin, CustomItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    public ItemStack create(Player player, String key) {
        return itemManager.createDisenchantStone(player, key);
    }

    public List<ItemStack> disenchant(Player player, ItemStack equipment, ItemStack stone, List<String> selectedEnchants) {
        String itemId = itemManager.identifyItem(stone);
        if (itemId == null) {
            return Collections.emptyList();
        }
        String configKey = itemManager.itemIdToTier(itemId);
        if (configKey == null) {
            return Collections.emptyList();
        }

        ConfigurationSection itemConfig = getConfigSection(configKey);
        if (itemConfig == null) {
            return Collections.emptyList();
        }

        int maxRemove = itemConfig.getInt("max-remove", 1);
        boolean selectable = itemConfig.getBoolean("selectable", false);
        int successChance = itemConfig.getInt("success-chance", 80);
        boolean destroyOnFail = itemConfig.getBoolean("destroy-on-fail", false);
        boolean keepLevel = itemConfig.getBoolean("keep-level", true);

        List<DisenchantTarget> available = collectTargets(equipment, configKey);
        if (available.isEmpty()) {
            return Collections.emptyList();
        }

        List<DisenchantTarget> toRemove = DisenchantTargetSelector.select(
                available,
                selectable,
                selectedEnchants,
                maxRemove,
                true
        );
        if (toRemove.isEmpty()) {
            return Collections.emptyList();
        }

        PDCManager pdcManager = plugin.getEnchantmentManager().getPdcManager();
        boolean changedEquipment = false;
        boolean strippedLore = false;
        ItemStack sourceEquipment = equipment.clone();
        List<ItemStack> results = new ArrayList<>();

        for (DisenchantTarget target : toRemove) {
            boolean success = ThreadLocalRandom.current().nextInt(100) < successChance;
            if (success) {
                if (target.type() == DisenchantTargetType.FOTIA && !strippedLore) {
                    EnchantmentLoreCleaner.stripGeneratedLore(plugin, player, equipment);
                    strippedLore = true;
                }
                if (removeTarget(equipment, target, pdcManager)) {
                    changedEquipment = true;
                }
                ItemStack book = createBook(player, target, keepLevel);
                if (book != null) {
                    results.add(book);
                }
                sendSuccessMessage(player, target);
                continue;
            }

            if (destroyOnFail) {
                if (target.type() == DisenchantTargetType.FOTIA && !strippedLore) {
                    EnchantmentLoreCleaner.stripGeneratedLore(plugin, player, equipment);
                    strippedLore = true;
                }
                if (removeTarget(equipment, target, pdcManager)) {
                    changedEquipment = true;
                }
                plugin.getMessageHelper().sendMessage(player, "disenchant-destroyed");
            } else {
                plugin.getMessageHelper().sendMessage(player, "disenchant-fail");
            }
        }

        if (changedEquipment) {
            EnchantmentLoreCleaner.applyGeneratedLoreFromSource(plugin, player, equipment, sourceEquipment);
        }
        return results;
    }

    public List<DisenchantTarget> collectTargets(ItemStack equipment, String configKey) {
        ConfigurationSection config = getConfigSection(configKey);
        if (config == null || equipment == null || equipment.getType().isAir()) {
            return Collections.emptyList();
        }

        DisenchantSource source = DisenchantItemRegistry.source(config);
        List<DisenchantTarget> result = new ArrayList<>();
        if (source.allows(DisenchantTargetType.FOTIA)) {
            Map<String, Integer> customEnchants = plugin.getEnchantmentManager()
                    .getPdcManager()
                    .getEnchantments(equipment);
            for (Map.Entry<String, Integer> entry : customEnchants.entrySet()) {
                DisenchantTarget target = new DisenchantTarget(
                        DisenchantTargetType.FOTIA,
                        entry.getKey(),
                        entry.getValue()
                );
                if (canDisenchant(configKey, target)) {
                    result.add(target);
                }
            }
        }
        if (source.allows(DisenchantTargetType.VANILLA)) {
            for (DisenchantTarget target : vanillaTargets(equipment)) {
                if (canDisenchant(configKey, target)) {
                    result.add(target);
                }
            }
        }
        return result;
    }

    public List<DisenchantTarget> selectTargets(ItemStack equipment,
                                                String configKey,
                                                List<String> selectedEnchants,
                                                boolean shuffle) {
        ConfigurationSection config = getConfigSection(configKey);
        if (config == null || equipment == null || equipment.getType().isAir()) {
            return Collections.emptyList();
        }
        return DisenchantTargetSelector.select(
                collectTargets(equipment, configKey),
                config.getBoolean("selectable", false),
                selectedEnchants,
                Math.max(1, config.getInt("max-remove", 1)),
                shuffle
        );
    }

    public boolean canDisenchant(String configKey, String enchantId) {
        return canDisenchant(configKey, new DisenchantTarget(DisenchantTargetType.FOTIA, enchantId, 1));
    }

    public boolean canDisenchant(String configKey, DisenchantTarget target) {
        ConfigurationSection config = getConfigSection(configKey);
        if (config == null || target == null || !DisenchantItemRegistry.source(config).allows(target.type())) {
            return false;
        }
        if (target.type() == DisenchantTargetType.VANILLA) {
            return true;
        }

        String maxRarity = config.getString("max-rarity", "divine");
        EnchantmentData enchantData = plugin.getEnchantmentManager().getEnchantment(target.id());
        if (enchantData == null) {
            return config.getBoolean("allow-vanilla", true);
        }

        String enchantRarity = enchantData.getRarity();
        if (enchantRarity == null) {
            return true;
        }

        int enchantRarityIndex = RARITY_ORDER.indexOf(enchantRarity.toLowerCase(Locale.ROOT));
        int maxRarityIndex = RARITY_ORDER.indexOf(maxRarity.toLowerCase(Locale.ROOT));
        return enchantRarityIndex == -1 || maxRarityIndex == -1 || enchantRarityIndex <= maxRarityIndex;
    }

    public int getMaxRemoveCount(String configKey) {
        ConfigurationSection config = getConfigSection(configKey);
        return config == null ? 1 : config.getInt("max-remove", 1);
    }

    public int getSuccessRate(String configKey) {
        ConfigurationSection config = getConfigSection(configKey);
        return config == null ? 80 : config.getInt("success-chance", 80);
    }

    public boolean canSelect(String configKey) {
        ConfigurationSection config = getConfigSection(configKey);
        return config != null && config.getBoolean("selectable", false);
    }

    public boolean isDisenchantStone(ItemStack item) {
        String type = itemManager.identifyItem(item);
        if (type == null) {
            return false;
        }
        YamlConfiguration itemsConfig = plugin.getConfigManager().getItemsConfig();
        return DisenchantItemRegistry.isDisenchantItem(itemsConfig, type);
    }

    public String getStoneTier(ItemStack item) {
        String type = itemManager.identifyItem(item);
        if (type == null) {
            return null;
        }
        return itemManager.itemIdToTier(type);
    }

    private List<DisenchantTarget> vanillaTargets(ItemStack equipment) {
        if (equipment == null || !equipment.hasItemMeta()) {
            return Collections.emptyList();
        }
        ItemMeta meta = equipment.getItemMeta();
        if (meta == null) {
            return Collections.emptyList();
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            addVanillaTarget(result, entry.getKey(), entry.getValue());
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<Enchantment, Integer> entry : storageMeta.getStoredEnchants().entrySet()) {
                addVanillaTarget(result, entry.getKey(), entry.getValue());
            }
        }
        List<DisenchantTarget> targets = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : result.entrySet()) {
            targets.add(new DisenchantTarget(DisenchantTargetType.VANILLA, entry.getKey(), entry.getValue()));
        }
        return targets;
    }

    private void addVanillaTarget(Map<String, Integer> result, Enchantment enchantment, int level) {
        if (enchantment == null || level <= 0) {
            return;
        }
        NamespacedKey key = enchantment.getKey();
        if (key != null && "minecraft".equals(key.getNamespace())) {
            result.merge(key.toString(), level, Math::max);
        }
    }

    private boolean removeTarget(ItemStack equipment, DisenchantTarget target, PDCManager pdcManager) {
        if (target.type() == DisenchantTargetType.FOTIA) {
            pdcManager.removeEnchantment(equipment, target.id());
            return true;
        }
        return removeVanillaEnchantment(equipment, target.id());
    }

    private boolean removeVanillaEnchantment(ItemStack equipment, String enchantId) {
        if (equipment == null || enchantId == null || !equipment.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = equipment.getItemMeta();
        Enchantment enchantment = resolveVanillaEnchantment(enchantId);
        if (meta == null || enchantment == null) {
            return false;
        }
        boolean modified;
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            modified = storageMeta.removeStoredEnchant(enchantment);
        } else {
            modified = meta.removeEnchant(enchantment);
        }
        if (modified) {
            equipment.setItemMeta(meta);
        }
        return modified;
    }

    private ItemStack createBook(Player player, DisenchantTarget target, boolean keepLevel) {
        int bookLevel = keepLevel ? target.level() : 1;
        if (target.type() == DisenchantTargetType.FOTIA) {
            EnchantmentData enchantData = plugin.getEnchantmentManager().getEnchantment(target.id());
            return enchantData == null ? null : itemManager.getStellarisCodex().createEnchantedBook(player, enchantData, bookLevel);
        }
        Enchantment enchantment = resolveVanillaEnchantment(target.id());
        if (enchantment == null) {
            return null;
        }
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            storageMeta.addStoredEnchant(enchantment, bookLevel, true);
            book.setItemMeta(storageMeta);
        }
        return book;
    }

    private void sendSuccessMessage(Player player, DisenchantTarget target) {
        String enchantName = target.type() == DisenchantTargetType.FOTIA
                ? plugin.getLanguageManager().getEnchantName(player, target.id())
                : target.id();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("enchant_name", enchantName);
        placeholders.put("level", toRoman(target.level()));
        plugin.getMessageHelper().sendMessage(player, "disenchant-success", placeholders);
    }

    private Enchantment resolveVanillaEnchantment(String id) {
        NamespacedKey key = namespacedKey(id);
        if (key == null || !"minecraft".equals(key.getNamespace())) {
            return null;
        }
        return BukkitRegistryCompat.enchantment(key);
    }

    private NamespacedKey namespacedKey(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        int colon = normalized.indexOf(':');
        if (colon < 0) {
            return NamespacedKey.minecraft(normalized);
        }
        if (colon == 0 || colon == normalized.length() - 1) {
            return null;
        }
        return new NamespacedKey(normalized.substring(0, colon), normalized.substring(colon + 1));
    }

    private ConfigurationSection getConfigSection(String key) {
        YamlConfiguration itemsConfig = plugin.getConfigManager().getItemsConfig();
        ConfigurationSection section = DisenchantItemRegistry.sectionForConfigKey(itemsConfig, key);
        if (section != null) {
            return section;
        }
        return DisenchantItemRegistry.section(itemsConfig, key);
    }

    private String toRoman(int num) {
        if (num <= 0) {
            return String.valueOf(num);
        }
        String[] ones = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return num <= 10 ? ones[num] : String.valueOf(num);
    }
}
