package gg.fotia.enchantment.lore.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class EnchantmentGeneratedLoreStripper {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private EnchantmentGeneratedLoreStripper() {
    }

    public static List<Component> stripGeneratedLoreCopies(List<Component> existingLore,
                                                           List<Component> generatedLore) {
        if (existingLore == null || existingLore.isEmpty()) {
            return List.of();
        }
        if (generatedLore == null || generatedLore.isEmpty()) {
            return new ArrayList<>(existingLore);
        }

        Set<String> generatedDisplayBases = generatedDisplayBases(generatedLore);
        int cursor = 0;
        boolean changed;
        do {
            changed = false;
            while (startsWith(existingLore, cursor, generatedLore)) {
                cursor += generatedLore.size();
                cursor = skipSingleBlank(existingLore, cursor);
                changed = true;
            }

            int staleEnd = staleGeneratedBlockEnd(existingLore, cursor, generatedDisplayBases);
            if (staleEnd > cursor) {
                cursor = staleEnd;
                changed = true;
            }
        } while (changed && cursor < existingLore.size());

        return new ArrayList<>(existingLore.subList(cursor, existingLore.size()));
    }

    private static Set<String> generatedDisplayBases(List<Component> generatedLore) {
        Set<String> bases = new HashSet<>();
        for (Component component : generatedLore) {
            String plain = semanticPlain(component).trim();
            if (plain.isEmpty() || isGeneratedDescriptionLine(plain)) {
                continue;
            }
            bases.add(displayBase(plain));
        }
        return bases;
    }

    private static int staleGeneratedBlockEnd(List<Component> lore, int offset, Set<String> generatedDisplayBases) {
        if (offset < 0 || offset >= lore.size() || generatedDisplayBases.isEmpty()) {
            return offset;
        }

        String first = semanticPlain(lore.get(offset)).trim();
        if (!generatedDisplayBases.contains(displayBase(first))) {
            return offset;
        }

        int cursor = offset + 1;
        while (cursor < lore.size()) {
            String plain = semanticPlain(lore.get(cursor)).trim();
            if (plain.isEmpty()) {
                return skipSingleBlank(lore, cursor);
            }
            if (!isGeneratedDescriptionLine(plain)) {
                break;
            }
            cursor++;
        }
        return cursor;
    }

    private static int skipSingleBlank(List<Component> lore, int cursor) {
        if (cursor < lore.size() && semanticPlain(lore.get(cursor)).trim().isEmpty()) {
            return cursor + 1;
        }
        return cursor;
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

    private static boolean isGeneratedDescriptionLine(String plain) {
        return plain.startsWith("- ") || plain.startsWith(" - ");
    }

    private static String displayBase(String plain) {
        String value = plain == null ? "" : plain.trim();
        int space = value.lastIndexOf(' ');
        if (space <= 0 || space >= value.length() - 1) {
            return value.toLowerCase(Locale.ROOT);
        }

        String lastToken = value.substring(space + 1);
        if (lastToken.matches("[IVXLCDM]+") || lastToken.matches("\\d+")) {
            return value.substring(0, space).trim().toLowerCase(Locale.ROOT);
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static String plain(Component component) {
        return component == null ? "" : PLAIN.serialize(component);
    }

    private static String semanticPlain(Component component) {
        String value = plain(component);
        String trimmed = value.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return value;
        }

        try {
            JsonObject object = JsonParser.parseString(trimmed).getAsJsonObject();
            if (object.has("text")) {
                return object.get("text").getAsString();
            }
        } catch (RuntimeException ignored) {
            return value;
        }
        return value;
    }
}
