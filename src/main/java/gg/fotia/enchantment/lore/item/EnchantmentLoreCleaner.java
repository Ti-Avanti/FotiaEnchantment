package gg.fotia.enchantment.lore.item;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.config.VanillaConfig.VanillaOverride;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.EnchantmentItemSanitizer;
import gg.fotia.enchantment.core.EnchantmentLimitPolicy;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.core.EnchantmentRegistry;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.lore.description.EnchantmentDescriptionLines;
import gg.fotia.enchantment.util.LegacyColorConverter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EnchantmentLoreCleaner {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private EnchantmentLoreCleaner() {
    }

    public static boolean stripGeneratedLore(FotiaEnchantment plugin, Player player, ItemStack item) {
        if (plugin == null || player == null || item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        List<Component> existingLore = meta.lore();
        if (existingLore == null || existingLore.isEmpty()) {
            return false;
        }

        List<Component> generatedLore = generatedLore(plugin, player, item, meta, true);
        if (generatedLore.isEmpty()) {
            return false;
        }

        List<Component> retainedLore = stripGeneratedLoreCopies(existingLore, generatedLore);
        if (retainedLore.equals(existingLore)) {
            return false;
        }

        meta.lore(retainedLore.isEmpty() ? null : retainedLore);
        item.setItemMeta(meta);
        return true;
    }

    public static List<Component> stripGeneratedLoreCopies(List<Component> existingLore, List<Component> generatedLore) {
        return EnchantmentGeneratedLoreStripper.stripGeneratedLoreCopies(existingLore, generatedLore);
    }

    public static List<Component> mergeGeneratedLore(List<Component> existingLore, List<Component> generatedLore) {
        if (generatedLore == null || generatedLore.isEmpty()) {
            return existingLore == null ? List.of() : new ArrayList<>(existingLore);
        }

        List<Component> retainedLore = stripGeneratedLoreCopies(existingLore, generatedLore);
        List<Component> mergedLore = new ArrayList<>(generatedLore);
        if (!retainedLore.isEmpty()) {
            mergedLore.add(Component.empty());
            mergedLore.addAll(retainedLore);
        }
        return mergedLore;
    }

    public static boolean applyGeneratedLore(FotiaEnchantment plugin, Player player, ItemStack item) {
        if (plugin == null || item == null || item.getType().isAir()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        List<Component> generatedLore = generatedLore(plugin, player, item, meta, false);
        if (generatedLore.isEmpty()) {
            return false;
        }

        List<Component> mergedLore = mergeGeneratedLore(meta.lore(), generatedLore);
        if (mergedLore.equals(meta.lore())) {
            return false;
        }

        meta.lore(mergedLore);
        item.setItemMeta(meta);
        return true;
    }

    private static List<Component> generatedLore(FotiaEnchantment plugin,
                                                 Player player,
                                                 ItemStack item,
                                                 ItemMeta meta,
                                                 boolean includeInvalidCustom) {
        EnchantmentManager enchantManager = plugin.getEnchantmentManager();
        if (enchantManager == null) {
            return List.of();
        }

        PDCManager pdc = enchantManager.getPdcManager();
        Map<String, LoreEntry> entries = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : pdc.getEnchantments(item).entrySet()) {
            String id = normalizeId(entry.getKey());
            int level = entry.getValue();
            EnchantmentData data = enchantManager.getEnchantment(id);
            if (!id.isEmpty() && level > 0
                    && (includeInvalidCustom || EnchantmentItemSanitizer.isValid(data, item.getType(), level))) {
                entries.put("custom:" + id, new LoreEntry(id, level, true, null, data));
            }
        }

        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            addVanillaEntry(entries, entry.getKey(), entry.getValue());
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<Enchantment, Integer> entry : storageMeta.getStoredEnchants().entrySet()) {
                addVanillaEntry(entries, entry.getKey(), entry.getValue());
            }
        }

        YamlConfiguration rarityConfig = plugin.getConfigManager().getRarityConfig();
        List<Component> generated = new ArrayList<>();
        List<LoreEntry> sortedEntries = new ArrayList<>(entries.values());
        sortedEntries.sort(loreEntryComparator(rarityConfig));
        for (LoreEntry entry : sortedEntries) {
            generated.add(deserialize(displayNameLine(plugin, player, entry, rarityConfig)));
            for (String description : descriptionLines(plugin, player, entry)) {
                generated.add(deserialize(EnchantmentLoreFormatter.descriptionLine(description)));
            }
        }
        for (String slotLine : slotLines(plugin, player, item, sortedEntries.size())) {
            generated.add(deserialize(slotLine));
        }
        return generated;
    }

    private static List<String> slotLines(FotiaEnchantment plugin, Player player, ItemStack item, int usedSlots) {
        if (plugin.getConfigManager() == null
                || plugin.getLanguageManager() == null
                || plugin.getEnchantmentManager() == null) {
            return List.of();
        }
        if (usedSlots <= 0
                && !EnchantmentLimitPolicy.hasKnownItemGroup(item.getType())
                && plugin.getEnchantmentManager().getApplicable(item).isEmpty()) {
            return List.of();
        }
        int maxSlots = plugin.getConfigManager().getMaxEnchantmentsForMaterial(item.getType());
        String emptySlot = plugin.getLanguageManager().getMessage(player, "enchant-slot-empty");
        if ("enchant-slot-empty".equals(emptySlot)) {
            emptySlot = EnchantmentSlotLore.FALLBACK_EMPTY_SLOT;
        }
        String summarySlot = plugin.getLanguageManager().getMessage(player, "enchant-slot-summary");
        if ("enchant-slot-summary".equals(summarySlot)) {
            summarySlot = EnchantmentSlotLore.FALLBACK_SLOT_SUMMARY;
        }
        return EnchantmentSlotLore.slotLines(
                maxSlots,
                usedSlots,
                plugin.getConfigManager().getEnchantSlotDisplayMode(),
                emptySlot,
                summarySlot);
    }

    private static Comparator<LoreEntry> loreEntryComparator(YamlConfiguration rarityConfig) {
        return Comparator
                .comparingInt((LoreEntry entry) -> entry.custom()
                        ? EnchantmentRarityOrder.rank(
                                rarityConfig,
                                entry.data() == null ? null : entry.data().getRarity())
                        : Integer.MAX_VALUE)
                .thenComparing(entry -> entry.custom() ? 0 : 1)
                .thenComparing(LoreEntry::id);
    }

    private static void addVanillaEntry(Map<String, LoreEntry> entries, Enchantment enchantment, int level) {
        if (enchantment == null || level <= 0) {
            return;
        }
        NamespacedKey key = enchantment.getKey();
        if (key == null || EnchantmentRegistry.getNamespace().equals(key.getNamespace())) {
            return;
        }
        if (!"minecraft".equals(key.getNamespace())) {
            return;
        }
        String id = normalizeId(key.getKey());
        entries.putIfAbsent("vanilla:" + id, new LoreEntry(id, level, false, enchantment, null));
    }

    private static String displayNameLine(FotiaEnchantment plugin,
                                          Player player,
                                          LoreEntry entry,
                                          YamlConfiguration rarityConfig) {
        if (entry.custom()) {
            String name = plugin.getLanguageManager().getEnchantName(player, entry.id());
            String rarityColor = "<white>";
            if (entry.data() != null && entry.data().getRarity() != null) {
                rarityColor = rarityConfig.getString(entry.data().getRarity() + ".color", "<white>");
            }
            boolean curse = entry.data() != null && entry.data().isCurse();
            return EnchantmentLoreFormatter.customDisplayLine(name, entry.level(), rarityColor, curse);
        }

        VanillaOverride override = vanillaOverride(plugin, entry.id());
        String name = override != null ? override.getDisplayName() : entry.id();
        boolean curse = entry.enchantment() != null && entry.enchantment().isCursed();
        return EnchantmentLoreFormatter.vanillaDisplayLine(name, entry.level(), curse);
    }

    private static List<String> descriptionLines(FotiaEnchantment plugin, Player player, LoreEntry entry) {
        if (entry.custom()) {
            List<String> description = plugin.getLanguageManager().getEnchantDescription(player, entry.id());
            return EnchantmentDescriptionLines.customDescriptionOrGenerated(
                    description,
                    entry.data(),
                    entry.level(),
                    key -> plugin.getLanguageManager().getGUIText(player, key),
                    "Unconfigured enchantment description."
            );
        }

        VanillaOverride override = vanillaOverride(plugin, entry.id());
        if (override != null && override.getDescription() != null && !override.getDescription().isEmpty()) {
            return override.getDescription();
        }
        return List.of("Vanilla enchantment.");
    }

    private static VanillaOverride vanillaOverride(FotiaEnchantment plugin, String id) {
        if (plugin.getVanillaManager() == null || plugin.getVanillaManager().getVanillaConfig() == null) {
            return null;
        }
        return plugin.getVanillaManager().getVanillaConfig().getOverride(id);
    }

    private static Component deserialize(String text) {
        return MINI_MESSAGE.deserialize(LegacyColorConverter.convert(text));
    }

    private static String normalizeId(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }

    private record LoreEntry(String id,
                             int level,
                             boolean custom,
                             Enchantment enchantment,
                             EnchantmentData data) {
    }
}
