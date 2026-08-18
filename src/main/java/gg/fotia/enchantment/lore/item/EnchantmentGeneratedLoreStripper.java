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
import java.util.regex.Pattern;

public final class EnchantmentGeneratedLoreStripper {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    /** 罗马数字/阿拉伯数字等级后缀匹配, 预编译避免热路径每行现场编译正则 */
    private static final Pattern ROMAN_TOKEN = Pattern.compile("[IVXLCDM]+");
    private static final Pattern NUMBER_TOKEN = Pattern.compile("\\d+");

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
        // 每行的纯文本只序列化一次, 循环内不再重复做 PlainText/JSON 解析
        String[] plainLines = new String[existingLore.size()];
        for (int i = 0; i < plainLines.length; i++) {
            plainLines[i] = semanticPlain(existingLore.get(i));
        }

        int cursor = 0;
        boolean changed;
        do {
            changed = false;
            while (startsWith(existingLore, cursor, generatedLore)) {
                cursor += generatedLore.size();
                cursor = skipSingleBlank(plainLines, cursor);
                changed = true;
            }

            int staleEnd = staleGeneratedBlockEnd(plainLines, cursor, generatedDisplayBases);
            if (staleEnd > cursor) {
                cursor = staleEnd;
                changed = true;
            }
        } while (changed && cursor < existingLore.size());

        return new ArrayList<>(existingLore.subList(cursor, existingLore.size()));
    }

    public static List<Component> stripGeneratedLoreVariants(List<Component> existingLore,
                                                             List<List<Component>> generatedLoreVariants) {
        if (generatedLoreVariants == null || generatedLoreVariants.isEmpty()) {
            return existingLore == null ? List.of() : new ArrayList<>(existingLore);
        }

        List<Component> combined = new ArrayList<>();
        for (List<Component> variant : generatedLoreVariants) {
            if (variant != null && !variant.isEmpty()) {
                combined.addAll(variant);
            }
        }
        return stripGeneratedLoreCopies(existingLore, combined);
    }

    private static Set<String> generatedDisplayBases(List<Component> generatedLore) {
        Set<String> bases = new HashSet<>();
        for (Component component : generatedLore) {
            String plain = semanticPlain(component);
            String trimmed = plain.trim();
            if (trimmed.isEmpty() || isGeneratedDescriptionLine(plain)) {
                continue;
            }
            bases.add(displayBase(trimmed));
        }
        return bases;
    }

    private static int staleGeneratedBlockEnd(String[] plainLines, int offset, Set<String> generatedDisplayBases) {
        if (offset < 0 || offset >= plainLines.length || generatedDisplayBases.isEmpty()) {
            return offset;
        }

        String first = plainLines[offset].trim();
        if (!generatedDisplayBases.contains(displayBase(first))) {
            return offset;
        }

        int cursor = offset + 1;
        while (cursor < plainLines.length) {
            String plain = plainLines[cursor];
            String trimmed = plain.trim();
            if (trimmed.isEmpty()) {
                return skipSingleBlank(plainLines, cursor);
            }
            if (generatedDisplayBases.contains(displayBase(trimmed))) {
                return cursor;
            }
            if (!isGeneratedDescriptionLine(plain)) {
                break;
            }
            cursor++;
        }
        return cursor;
    }

    private static int skipSingleBlank(String[] plainLines, int cursor) {
        if (cursor < plainLines.length && plainLines[cursor].trim().isEmpty()) {
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
        if (plain == null || plain.isEmpty()) {
            return false;
        }
        if (Character.isWhitespace(plain.charAt(0))) {
            return true;
        }
        String trimmed = plain.trim();
        return trimmed.startsWith("- ");
    }

    private static String displayBase(String plain) {
        String value = plain == null ? "" : plain.trim();
        int space = value.lastIndexOf(' ');
        if (space <= 0 || space >= value.length() - 1) {
            return value.toLowerCase(Locale.ROOT);
        }

        String lastToken = value.substring(space + 1);
        if (ROMAN_TOKEN.matcher(lastToken).matches() || NUMBER_TOKEN.matcher(lastToken).matches()) {
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
