package gg.fotia.enchantment.lore.item;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.compat.BukkitRegistryCompat;
import gg.fotia.enchantment.config.VanillaConfig.VanillaOverride;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.EnchantmentItemSanitizer;
import gg.fotia.enchantment.core.EnchantmentLimitPolicy;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.core.EnchantmentRegistry;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.lore.description.EnchantmentDescriptionLines;
import gg.fotia.enchantment.util.MiniMessageCache;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按显式语言渲染附魔 Lore。调用方决定使用服务端默认语言还是玩家语言。
 */
public final class LocalizedEnchantmentLoreRenderer {

    private static final int RENDER_CACHE_MAX = 2048;
    private static final int SLOT_CACHE_MAX = 256;
    private static volatile Map<RenderKey, List<Component>> renderCache = new ConcurrentHashMap<>();
    private static volatile Map<String, List<Component>> slotCache = new ConcurrentHashMap<>();

    private LocalizedEnchantmentLoreRenderer() {
    }

    public static List<Component> render(FotiaEnchantment plugin,
                                         String locale,
                                         ItemStack item,
                                         boolean includeInvalidCustom,
                                         boolean includeDisabledVanilla) {
        if (plugin == null || item == null || item.getType().isAir()) {
            return List.of();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return List.of();
        }
        return render(plugin, locale, item, meta, includeInvalidCustom, includeDisabledVanilla);
    }

    public static List<List<Component>> renderAllLocales(FotiaEnchantment plugin,
                                                          ItemStack item,
                                                          boolean includeInvalidCustom,
                                                          boolean includeDisabledVanilla) {
        if (plugin == null || plugin.getLanguageManager() == null
                || item == null || item.getType().isAir() || plugin.getEnchantmentManager() == null) {
            return List.of();
        }
        Map<RenderKey, List<Component>> cache = renderCache;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return List.of();
        }
        List<LoreEntry> entries = collectEntries(plugin, item, meta,
                plugin.getEnchantmentManager(), includeInvalidCustom, includeDisabledVanilla);
        entries.sort(entryComparator(plugin));
        String signature = entrySignature(entries);
        Set<String> locales = plugin.getLanguageManager().getAvailableLocales();
        List<List<Component>> variants = new ArrayList<>(locales.size());
        for (String locale : locales) {
            List<Component> lore = renderPrepared(plugin, locale, item, entries, signature,
                    includeInvalidCustom, includeDisabledVanilla, cache);
            if (!lore.isEmpty()) {
                variants.add(lore);
            }
        }
        return List.copyOf(variants);
    }

    public static List<Component> potentialSlotLore(FotiaEnchantment plugin, String locale, ItemStack item) {
        if (plugin == null || item == null || plugin.getConfigManager() == null
                || plugin.getLanguageManager() == null) {
            return List.of();
        }
        int maxSlots = plugin.getConfigManager().getMaxEnchantmentsForMaterial(item.getType());
        if (maxSlots < 0) {
            return List.of();
        }

        String emptySlot = plugin.getLanguageManager().getMessage(locale, "enchant-slot-empty");
        if ("enchant-slot-empty".equals(emptySlot)) {
            emptySlot = EnchantmentSlotLore.FALLBACK_EMPTY_SLOT;
        }
        String summarySlot = plugin.getLanguageManager().getMessage(locale, "enchant-slot-summary");
        if ("enchant-slot-summary".equals(summarySlot)) {
            summarySlot = EnchantmentSlotLore.FALLBACK_SLOT_SUMMARY;
        }

        String cacheKey = emptySlot + '\0' + summarySlot + '\0' + maxSlots;
        Map<String, List<Component>> cache = slotCache;
        List<Component> cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<Component> candidates = new ArrayList<>();
        candidates.add(deserialize(emptySlot));
        for (int usedSlots = 0; usedSlots <= maxSlots; usedSlots++) {
            candidates.add(deserialize(EnchantmentSlotLore.slotLines(
                    maxSlots,
                    usedSlots,
                    EnchantmentSlotLore.MODE_SUMMARY,
                    emptySlot,
                    summarySlot).getFirst()));
        }
        List<Component> immutable = List.copyOf(candidates);
        putBounded(cache, cacheKey, immutable, SLOT_CACHE_MAX);
        return immutable;
    }

    public static List<Component> potentialSlotLoreAllLocales(FotiaEnchantment plugin, ItemStack item) {
        if (plugin == null || plugin.getLanguageManager() == null) {
            return List.of();
        }
        List<Component> candidates = new ArrayList<>();
        for (String locale : plugin.getLanguageManager().getAvailableLocales()) {
            for (Component candidate : potentialSlotLore(plugin, locale, item)) {
                if (!candidates.contains(candidate)) {
                    candidates.add(candidate);
                }
            }
        }
        return List.copyOf(candidates);
    }

    public static void clearCaches() {
        // 替换缓存实例，旧渲染任务只能写回旧缓存，无法污染重载后的新缓存。
        renderCache = new ConcurrentHashMap<>();
        slotCache = new ConcurrentHashMap<>();
    }

    private static List<Component> render(FotiaEnchantment plugin,
                                          String locale,
                                          ItemStack item,
                                          ItemMeta meta,
                                          boolean includeInvalidCustom,
                                          boolean includeDisabledVanilla) {
        EnchantmentManager enchantManager = plugin.getEnchantmentManager();
        Map<RenderKey, List<Component>> cache = renderCache;
        if (enchantManager == null || plugin.getConfigManager() == null || plugin.getLanguageManager() == null) {
            return List.of();
        }

        List<LoreEntry> entries = collectEntries(
                plugin,
                item,
                meta,
                enchantManager,
                includeInvalidCustom,
                includeDisabledVanilla);
        entries.sort(entryComparator(plugin));

        return renderPrepared(plugin, locale, item, entries, entrySignature(entries),
                includeInvalidCustom, includeDisabledVanilla, cache);
    }

    private static List<Component> renderPrepared(FotiaEnchantment plugin,
                                                  String locale,
                                                  ItemStack item,
                                                  List<LoreEntry> entries,
                                                  String signature,
                                                  boolean includeInvalidCustom,
                                                  boolean includeDisabledVanilla,
                                                  Map<RenderKey, List<Component>> cache) {
        String normalizedLocale = locale == null || locale.isBlank()
                ? plugin.getLanguageManager().getDefaultLanguage()
                : locale.toLowerCase(Locale.ROOT).replace('-', '_');
        RenderKey cacheKey = new RenderKey(
                normalizedLocale,
                item.getType(),
                item.getMaxStackSize(),
                plugin.getConfigManager().getConfigGeneration(),
                plugin.getEnchantmentManager(),
                includeInvalidCustom,
                includeDisabledVanilla,
                signature);
        List<Component> cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        YamlConfiguration rarityConfig = plugin.getConfigManager().getRarityConfig();
        List<Component> generated = new ArrayList<>();
        for (LoreEntry entry : entries) {
            generated.add(deserialize(displayNameLine(plugin, normalizedLocale, entry, rarityConfig)));
            for (String description : descriptionLines(plugin, normalizedLocale, entry)) {
                generated.add(deserialize(EnchantmentLoreFormatter.descriptionLine(description)));
            }
        }
        generated.addAll(slotLines(plugin, normalizedLocale, item, entries.size()));

        List<Component> immutable = List.copyOf(generated);
        putBounded(cache, cacheKey, immutable, RENDER_CACHE_MAX);
        return immutable;
    }

    private static List<LoreEntry> collectEntries(FotiaEnchantment plugin,
                                                  ItemStack item,
                                                  ItemMeta meta,
                                                  EnchantmentManager enchantManager,
                                                  boolean includeInvalidCustom,
                                                  boolean includeDisabledVanilla) {
        PDCManager pdc = enchantManager.getPdcManager();
        Map<String, LoreEntry> entries = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : pdc.getEnchantments(meta).entrySet()) {
            String id = normalizeId(entry.getKey());
            int level = entry.getValue();
            EnchantmentData data = enchantManager.getEnchantment(id);
            if (!id.isEmpty() && level > 0
                    && (includeInvalidCustom || EnchantmentItemSanitizer.isValid(data, item.getType(), level))) {
                entries.put("custom:" + id, new LoreEntry(id, level, true, null, data));
            }
        }

        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            addVanillaEntry(plugin, entries, meta, entry.getKey(), entry.getValue(), includeDisabledVanilla);
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Map.Entry<Enchantment, Integer> entry : storageMeta.getStoredEnchants().entrySet()) {
                addVanillaEntry(plugin, entries, meta, entry.getKey(), entry.getValue(), includeDisabledVanilla);
            }
        }
        return new ArrayList<>(entries.values());
    }

    private static void addVanillaEntry(FotiaEnchantment plugin,
                                        Map<String, LoreEntry> entries,
                                        ItemMeta meta,
                                        Enchantment enchantment,
                                        int level,
                                        boolean includeDisabledVanilla) {
        if (enchantment == null || level <= 0 || isSyntheticGuiGlow(plugin, meta, enchantment)) {
            return;
        }
        NamespacedKey key = enchantment.getKey();
        if (key == null || EnchantmentRegistry.getNamespace().equals(key.getNamespace())
                || !"minecraft".equals(key.getNamespace())) {
            return;
        }
        if (!includeDisabledVanilla && plugin.getVanillaManager() != null
                && plugin.getVanillaManager().isDisabled(enchantment)) {
            return;
        }
        String id = normalizeId(key.getKey());
        entries.putIfAbsent("vanilla:" + id, new LoreEntry(id, level, false, enchantment, null));
    }

    private static boolean isSyntheticGuiGlow(FotiaEnchantment plugin, ItemMeta meta, Enchantment enchantment) {
        Enchantment unbreaking = BukkitRegistryCompat.unbreakingEnchantment();
        return enchantment.equals(unbreaking)
                && meta.getPersistentDataContainer().has(
                        new NamespacedKey(plugin, "gui_glow"),
                        PersistentDataType.BYTE);
    }

    private static List<Component> slotLines(FotiaEnchantment plugin,
                                             String locale,
                                             ItemStack item,
                                             int usedSlots) {
        boolean eligible = EnchantmentLimitPolicy.hasKnownItemGroup(item.getType())
                || !plugin.getEnchantmentManager().getApplicable(item).isEmpty();
        if (!EnchantmentDisplayPolicy.shouldDisplayEnchantSlotLore(
                usedSlots,
                eligible,
                item.getMaxStackSize())) {
            return List.of();
        }

        int maxSlots = plugin.getConfigManager().getMaxEnchantmentsForMaterial(item.getType());
        String emptySlot = plugin.getLanguageManager().getMessage(locale, "enchant-slot-empty");
        if ("enchant-slot-empty".equals(emptySlot)) {
            emptySlot = EnchantmentSlotLore.FALLBACK_EMPTY_SLOT;
        }
        String summarySlot = plugin.getLanguageManager().getMessage(locale, "enchant-slot-summary");
        if ("enchant-slot-summary".equals(summarySlot)) {
            summarySlot = EnchantmentSlotLore.FALLBACK_SLOT_SUMMARY;
        }

        List<Component> components = new ArrayList<>();
        for (String line : EnchantmentSlotLore.slotLines(
                maxSlots,
                usedSlots,
                plugin.getConfigManager().getEnchantSlotDisplayMode(),
                emptySlot,
                summarySlot)) {
            components.add(deserialize(line));
        }
        return components;
    }

    private static String displayNameLine(FotiaEnchantment plugin,
                                          String locale,
                                          LoreEntry entry,
                                          YamlConfiguration rarityConfig) {
        if (entry.custom()) {
            String name = plugin.getLanguageManager().getEnchantName(locale, entry.id());
            String rarityColor = entry.data() == null || entry.data().getRarity() == null
                    ? "<white>"
                    : rarityConfig.getString(entry.data().getRarity() + ".color", "<white>");
            boolean curse = entry.data() != null && entry.data().isCurse();
            return EnchantmentLoreFormatter.customDisplayLine(name, entry.level(), rarityColor, curse);
        }

        VanillaOverride override = vanillaOverride(plugin, entry.id());
        String name = override != null ? override.getDisplayName() : entry.id();
        boolean curse = entry.enchantment() != null && entry.enchantment().isCursed();
        return EnchantmentLoreFormatter.vanillaDisplayLine(name, entry.level(), curse);
    }

    private static List<String> descriptionLines(FotiaEnchantment plugin, String locale, LoreEntry entry) {
        if (entry.custom()) {
            return EnchantmentDescriptionLines.customDescriptionOrGenerated(
                    plugin.getLanguageManager().getEnchantDescription(locale, entry.id()),
                    entry.data(),
                    entry.level(),
                    key -> plugin.getLanguageManager().getGUIText(locale, key),
                    "Unconfigured enchantment description.");
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

    private static Comparator<LoreEntry> entryComparator(FotiaEnchantment plugin) {
        return Comparator
                .comparingInt((LoreEntry entry) -> entry.custom()
                        ? plugin.getConfigManager().getRarityRank(
                                entry.data() == null ? null : entry.data().getRarity())
                        : Integer.MAX_VALUE)
                .thenComparing(entry -> entry.custom() ? 0 : 1)
                .thenComparing(LoreEntry::id);
    }

    private static String entrySignature(List<LoreEntry> entries) {
        StringBuilder signature = new StringBuilder();
        for (LoreEntry entry : entries) {
            signature.append(entry.custom() ? 'c' : 'v')
                    .append(':').append(entry.id())
                    .append(':').append(entry.level())
                    .append(';');
        }
        return signature.toString();
    }

    private static Component deserialize(String text) {
        return MiniMessageCache.deserializeLegacyAware(text);
    }

    private static String normalizeId(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }

    private static <K, V> void putBounded(Map<K, V> cache, K key, V value, int maxSize) {
        if (cache.size() >= maxSize) {
            cache.clear();
        }
        cache.put(key, value);
    }

    private record RenderKey(String locale,
                              Material material,
                              int maxStackSize,
                              int configGeneration,
                              EnchantmentManager manager,
                             boolean includeInvalidCustom,
                             boolean includeDisabledVanilla,
                             String entries) {
    }

    private record LoreEntry(String id,
                             int level,
                             boolean custom,
                             Enchantment enchantment,
                             EnchantmentData data) {
    }
}
