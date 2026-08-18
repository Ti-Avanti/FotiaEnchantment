package gg.fotia.enchantment.lore.item;

import gg.fotia.enchantment.FotiaEnchantment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 管理真实物品中的规范化 Lore，并为数据包显示提供清理入口。
 */
public final class EnchantmentLoreCleaner {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final Pattern SLOT_SUMMARY_PATTERN = Pattern.compile("\\d+\\s*/\\s*\\d+");

    private EnchantmentLoreCleaner() {
    }

    public static boolean stripGeneratedLore(FotiaEnchantment plugin, Player player, ItemStack item) {
        if (plugin == null || item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        List<Component> originalLore = meta.lore();
        GeneratedLoreMarker.StripResult marked = GeneratedLoreMarker.strip(plugin, meta, originalLore);
        List<Component> retainedLore = stripLegacyGeneratedLore(plugin, item, marked.lore(), true, true);
        boolean markerChanged = GeneratedLoreMarker.clear(plugin, meta);
        boolean changed = markerChanged || !sameLore(originalLore, retainedLore);
        if (!changed) {
            return false;
        }

        meta.lore(retainedLore.isEmpty() ? null : retainedLore);
        item.setItemMeta(meta);
        return true;
    }

    public static List<Component> stripGeneratedLoreCopies(List<Component> existingLore,
                                                           List<Component> generatedLore) {
        return EnchantmentGeneratedLoreStripper.stripGeneratedLoreCopies(existingLore, generatedLore);
    }

    public static List<Component> mergeGeneratedLore(List<Component> existingLore,
                                                     List<Component> generatedLore) {
        if (generatedLore == null || generatedLore.isEmpty()) {
            return copy(existingLore);
        }
        return mergeGeneratedLore(existingLore, generatedLore, List.of());
    }

    public static List<Component> mergeGeneratedLore(List<Component> existingLore,
                                                     List<Component> generatedLore,
                                                     List<Component> sourceGeneratedLore) {
        List<Component> retainedLore = copy(existingLore);
        if (sourceGeneratedLore != null && !sourceGeneratedLore.isEmpty()) {
            retainedLore = stripLikelyLeadingSlotLoreCopies(retainedLore);
            retainedLore = stripGeneratedLoreCopies(retainedLore, sourceGeneratedLore);
        }

        if (generatedLore == null || generatedLore.isEmpty()) {
            return stripLikelyLeadingSlotLoreCopies(retainedLore);
        }

        retainedLore = stripLikelyLeadingSlotLoreCopies(
                stripGeneratedLoreCopies(retainedLore, generatedLore));
        return prependGeneratedLore(retainedLore, generatedLore);
    }

    /**
     * 真实物品始终使用服务端默认语言，避免持有者语言变化污染持久化数据。
     */
    public static boolean applyGeneratedLore(FotiaEnchantment plugin, Player player, ItemStack item) {
        return applyCanonicalLore(plugin, item, null);
    }

    public static boolean applyGeneratedLoreFromSource(FotiaEnchantment plugin,
                                                       Player player,
                                                       ItemStack item,
                                                       ItemStack source) {
        return applyCanonicalLore(plugin, item, source);
    }

    /**
     * 数据包层调用：只清理传出副本，不改变服务端原物品。
     */
    public static List<Component> stripAllGeneratedLore(FotiaEnchantment plugin,
                                                        ItemStack item,
                                                        ItemMeta meta,
                                                        List<Component> existingLore) {
        if (plugin == null || item == null || meta == null) {
            return copy(existingLore);
        }
        GeneratedLoreMarker.StripResult marked = GeneratedLoreMarker.strip(plugin, meta, existingLore);
        return stripLegacyGeneratedLore(plugin, item, marked.lore(), true, true);
    }

    public static List<Component> localizedGeneratedLore(FotiaEnchantment plugin,
                                                         Player player,
                                                         ItemStack item,
                                                         boolean includeInvalidCustom,
                                                         boolean includeDisabledVanilla) {
        if (plugin == null || plugin.getLanguageManager() == null) {
            return List.of();
        }
        return LocalizedEnchantmentLoreRenderer.render(
                plugin,
                plugin.getLanguageManager().getPlayerLocale(player),
                item,
                includeInvalidCustom,
                includeDisabledVanilla);
    }

    public static List<Component> potentialSlotLore(FotiaEnchantment plugin, Player player, ItemStack item) {
        if (plugin == null || plugin.getLanguageManager() == null) {
            return List.of();
        }
        return LocalizedEnchantmentLoreRenderer.potentialSlotLore(
                plugin,
                plugin.getLanguageManager().getPlayerLocale(player),
                item);
    }

    public static List<Component> stripPotentialSlotLoreCopies(List<Component> existingLore,
                                                               List<Component> slotLoreCandidates) {
        if (existingLore == null || existingLore.isEmpty()) {
            return List.of();
        }
        if (slotLoreCandidates == null || slotLoreCandidates.isEmpty()) {
            return new ArrayList<>(existingLore);
        }

        int cursor = 0;
        while (cursor < existingLore.size() && slotLoreCandidates.contains(existingLore.get(cursor))) {
            cursor++;
        }
        if (cursor > 0 && cursor < existingLore.size() && existingLore.get(cursor).equals(Component.empty())) {
            cursor++;
        }
        return new ArrayList<>(existingLore.subList(cursor, existingLore.size()));
    }

    public static void clearCaches() {
        LocalizedEnchantmentLoreRenderer.clearCaches();
    }

    private static boolean applyCanonicalLore(FotiaEnchantment plugin,
                                              ItemStack item,
                                              ItemStack source) {
        if (plugin == null || plugin.getLanguageManager() == null
                || item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        List<Component> originalLore = meta.lore();
        GeneratedLoreMarker.StripResult marked = GeneratedLoreMarker.strip(plugin, meta, originalLore);
        List<Component> retainedLore = stripLegacyGeneratedLore(plugin, item, marked.lore(), true, true);
        if (source != null && !source.getType().isAir()) {
            retainedLore = stripVariants(
                    retainedLore,
                    LocalizedEnchantmentLoreRenderer.renderAllLocales(plugin, source, true, true));
        }

        List<Component> generatedLore = LocalizedEnchantmentLoreRenderer.render(
                plugin,
                plugin.getLanguageManager().getDefaultLanguage(),
                item,
                false,
                false);
        List<Component> mergedLore = prependGeneratedLore(retainedLore, generatedLore);
        boolean markerChanged = GeneratedLoreMarker.write(plugin, meta, generatedLore);
        boolean changed = markerChanged || !sameLore(originalLore, mergedLore);
        if (!changed) {
            return false;
        }

        meta.lore(mergedLore.isEmpty() ? null : mergedLore);
        item.setItemMeta(meta);
        return true;
    }

    private static List<Component> stripLegacyGeneratedLore(FotiaEnchantment plugin,
                                                            ItemStack item,
                                                            List<Component> existingLore,
                                                            boolean includeInvalidCustom,
                                                            boolean includeDisabledVanilla) {
        List<Component> retainedLore = stripPotentialSlotLoreCopies(
                existingLore,
                LocalizedEnchantmentLoreRenderer.potentialSlotLoreAllLocales(plugin, item));
        retainedLore = stripVariants(
                retainedLore,
                LocalizedEnchantmentLoreRenderer.renderAllLocales(
                        plugin,
                        item,
                        includeInvalidCustom,
                        includeDisabledVanilla));
        return stripLikelyLeadingSlotLoreCopies(retainedLore);
    }

    private static List<Component> stripVariants(List<Component> existingLore,
                                                 List<List<Component>> variants) {
        return EnchantmentGeneratedLoreStripper.stripGeneratedLoreVariants(existingLore, variants);
    }

    private static List<Component> prependGeneratedLore(List<Component> retainedLore,
                                                        List<Component> generatedLore) {
        if (generatedLore == null || generatedLore.isEmpty()) {
            return copy(retainedLore);
        }
        List<Component> result = new ArrayList<>(generatedLore);
        if (retainedLore != null && !retainedLore.isEmpty()) {
            result.add(Component.empty());
            result.addAll(retainedLore);
        }
        return result;
    }

    private static List<Component> stripLikelyLeadingSlotLoreCopies(List<Component> existingLore) {
        if (existingLore == null || existingLore.isEmpty()) {
            return List.of();
        }
        int cursor = 0;
        while (cursor < existingLore.size() && isLikelySlotLore(existingLore.get(cursor))) {
            cursor++;
        }
        if (cursor > 0 && cursor < existingLore.size() && existingLore.get(cursor).equals(Component.empty())) {
            cursor++;
        }
        return new ArrayList<>(existingLore.subList(cursor, existingLore.size()));
    }

    private static boolean isLikelySlotLore(Component component) {
        String plain = component == null ? "" : PLAIN.serialize(component).trim();
        if (plain.isEmpty()) {
            return false;
        }
        String lower = plain.toLowerCase(Locale.ROOT);
        boolean namesSlot = lower.contains("slot")
                || plain.contains("槽位")
                || plain.contains("슬롯")
                || plain.contains("枠");
        if (!namesSlot) {
            return false;
        }
        return (plain.startsWith("[") && plain.endsWith("]"))
                || SLOT_SUMMARY_PATTERN.matcher(plain).find();
    }

    private static boolean sameLore(List<Component> first, List<Component> second) {
        return (first == null ? List.of() : first).equals(second == null ? List.of() : second);
    }

    private static List<Component> copy(List<Component> lore) {
        return lore == null ? List.of() : new ArrayList<>(lore);
    }
}
