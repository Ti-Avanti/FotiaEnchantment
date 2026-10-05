package gg.fotia.enchantment.lore.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

final class LoreFingerprint {

    private LoreFingerprint() {
    }

    static String canonical(List<Component> lore) {
        return fingerprint(lore, false);
    }

    static String legacy(List<Component> lore) {
        return fingerprint(lore, true);
    }

    private static String fingerprint(List<Component> lore, boolean legacy) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Component component : lore) {
                // JSON 保留样式和交互信息，避免 Adventure 4/5 的 toString 实现差异。
                String serialized = legacy ? String.valueOf(component)
                        : canonicalize(GsonComponentSerializer.gson().serializeToTree(component)).toString();
                byte[] value = serialized.getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (value.length >>> 24));
                digest.update((byte) (value.length >>> 16));
                digest.update((byte) (value.length >>> 8));
                digest.update((byte) value.length);
                digest.update(value);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static JsonElement canonicalize(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject sorted = new JsonObject();
            element.getAsJsonObject().entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .forEach(entry -> sorted.add(entry.getKey(), canonicalize(entry.getValue())));
            return sorted;
        }
        if (element.isJsonArray()) {
            JsonArray array = new JsonArray();
            for (JsonElement value : element.getAsJsonArray()) {
                array.add(canonicalize(value));
            }
            return array;
        }
        return element;
    }
}
