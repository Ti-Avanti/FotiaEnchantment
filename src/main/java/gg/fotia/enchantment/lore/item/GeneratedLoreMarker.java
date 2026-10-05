package gg.fotia.enchantment.lore.item;

import gg.fotia.enchantment.FotiaEnchantment;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

final class GeneratedLoreMarker {

    private static final String VERSION = "v2";
    private static final String LEGACY_VERSION = "v1";
    private static final String KEY = "generated_lore_meta";

    private GeneratedLoreMarker() {
    }

    static String encode(List<Component> generatedLore) {
        List<Component> normalized = generatedLore == null ? List.of() : generatedLore;
        return VERSION + ':' + normalized.size() + ':' + LoreFingerprint.canonical(normalized);
    }

    static List<Component> stripMarkedPrefix(List<Component> existingLore, String marker) {
        if (existingLore == null || existingLore.isEmpty() || marker == null || marker.isBlank()) {
            return existingLore == null ? List.of() : new ArrayList<>(existingLore);
        }

        String[] parts = marker.split(":", 3);
        if (parts.length != 3 || (!VERSION.equals(parts[0]) && !LEGACY_VERSION.equals(parts[0]))) {
            return new ArrayList<>(existingLore);
        }

        int lineCount;
        try {
            lineCount = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ignored) {
            return new ArrayList<>(existingLore);
        }
        if (lineCount <= 0 || lineCount > existingLore.size()) {
            return new ArrayList<>(existingLore);
        }

        List<Component> prefix = existingLore.subList(0, lineCount);
        String fingerprint = LEGACY_VERSION.equals(parts[0])
                ? LoreFingerprint.legacy(prefix) : LoreFingerprint.canonical(prefix);
        if (!parts[2].equals(fingerprint)) {
            return new ArrayList<>(existingLore);
        }

        int cursor = lineCount;
        if (cursor < existingLore.size() && Component.empty().equals(existingLore.get(cursor))) {
            cursor++;
        }
        return new ArrayList<>(existingLore.subList(cursor, existingLore.size()));
    }

    static StripResult strip(FotiaEnchantment plugin, ItemMeta meta, List<Component> existingLore) {
        NamespacedKey key = new NamespacedKey(plugin, KEY);
        String marker = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (marker == null) {
            return new StripResult(copy(existingLore), false);
        }

        List<Component> stripped = stripMarkedPrefix(existingLore, marker);
        return new StripResult(stripped, false);
    }

    static boolean clear(FotiaEnchantment plugin, ItemMeta meta) {
        NamespacedKey key = new NamespacedKey(plugin, KEY);
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return false;
        }
        meta.getPersistentDataContainer().remove(key);
        return true;
    }

    static boolean write(FotiaEnchantment plugin, ItemMeta meta, List<Component> generatedLore) {
        NamespacedKey key = new NamespacedKey(plugin, KEY);
        if (generatedLore == null || generatedLore.isEmpty()) {
            if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                return false;
            }
            meta.getPersistentDataContainer().remove(key);
            return true;
        }

        String marker = encode(generatedLore);
        String existing = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (marker.equals(existing)) {
            return false;
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, marker);
        return true;
    }

    private static List<Component> copy(List<Component> lore) {
        return lore == null ? List.of() : new ArrayList<>(lore);
    }

    record StripResult(List<Component> lore, boolean metadataChanged) {
    }
}
