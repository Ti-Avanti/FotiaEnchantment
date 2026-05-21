package gg.fotia.enchantment.lore;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.config.VanillaConfig.VanillaOverride;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.core.EnchantmentRegistry;
import gg.fotia.enchantment.core.PDCManager;
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

        List<Component> generatedLore = generatedLore(plugin, player, item, meta);
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
        if (existingLore == null || existingLore.isEmpty()) {
            return List.of();
        }
        if (generatedLore == null || generatedLore.isEmpty()) {
            return new ArrayList<>(existingLore);
        }

        int cursor = 0;
        while (startsWith(existingLore, cursor, generatedLore)) {
            cursor += generatedLore.size();
            if (cursor < existingLore.size() && Component.empty().equals(existingLore.get(cursor))) {
                cursor++;
            }
        }
        return new ArrayList<>(existingLore.subList(cursor, existingLore.size()));
    }

    private static List<Component> generatedLore(FotiaEnchantment plugin, Player player, ItemStack item, ItemMeta meta) {
        EnchantmentManager enchantManager = plugin.getEnchantmentManager();
        if (enchantManager == null) {
            return List.of();
        }

        PDCManager pdc = enchantManager.getPdcManager();
        Map<String, LoreEntry> entries = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : pdc.getEnchantments(item).entrySet()) {
            String id = normalizeId(entry.getKey());
            int level = entry.getValue();
            if (!id.isEmpty() && level > 0) {
                entries.put("custom:" + id, new LoreEntry(id, level, true, null, enchantManager.getEnchantment(id)));
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

        if (entries.isEmpty()) {
            return List.of();
        }

        YamlConfiguration rarityConfig = plugin.getConfigManager().getRarityConfig();
        List<Component> generated = new ArrayList<>();
        for (LoreEntry entry : entries.values()) {
            generated.add(deserialize(displayNameLine(plugin, player, entry, rarityConfig)));
            for (String description : descriptionLines(plugin, player, entry)) {
                generated.add(deserialize(EnchantmentLoreFormatter.descriptionLine(description)));
            }
        }
        return generated;
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
            if (entry.data() != null) {
                List<String> generated = EnchantmentEffectDescriptionFormatter.renderLines(
                        entry.data(),
                        entry.level(),
                        key -> plugin.getLanguageManager().getGUIText(player, key)
                );
                if (!generated.isEmpty()) {
                    return generated;
                }
            }
            List<String> description = plugin.getLanguageManager().getEnchantDescription(player, entry.id());
            return description.isEmpty() ? List.of("Unconfigured enchantment description.") : description;
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

    private static boolean startsWith(List<Component> lore, int offset, List<Component> prefix) {
        if (offset < 0 || offset + prefix.size() > lore.size()) {
            return false;
        }
        for (int i = 0; i < prefix.size(); i++) {
            if (!prefix.get(i).equals(lore.get(offset + i))) {
                return false;
            }
        }
        return true;
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
