package gg.fotia.enchantment.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 旧版颜色码转换器
 * 将 &/§ 颜色码和 HEX 颜色码转换为 MiniMessage 标签
 */
public class LegacyColorConverter {

    // 匹配 &#RRGGBB 格式
    private static final Pattern HEX_AMPERSAND_PATTERN = Pattern.compile("[&\u00a7]#([0-9a-fA-F]{6})");
    // 匹配 §x§R§R§G§G§B§B 格式
    private static final Pattern HEX_SECTION_PATTERN = Pattern.compile("\u00a7x(\u00a7[0-9a-fA-F]){6}");
    // 匹配 &X 或 §X 格式的单字符颜色码
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("[&\u00a7]([0-9a-fk-orA-FK-OR])");

    /**
     * 将包含旧版颜色码的文本转换为 MiniMessage 格式
     *
     * @param text 原始文本（可能含有 &/§ 颜色码）
     * @return 转换后的 MiniMessage 格式文本
     */
    public static String convert(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 1. 转换 §x§R§R§G§G§B§B 格式为 <color:#RRGGBB>
        text = convertSectionHex(text);

        // 2. 转换 &#RRGGBB 或 §#RRGGBB 格式为 <color:#RRGGBB>
        text = convertAmpersandHex(text);

        // 3. 转换 &X / §X 单字符颜色码为 MiniMessage 标签
        text = convertLegacyCodes(text);

        return text;
    }

    private static String convertSectionHex(String text) {
        Matcher matcher = HEX_SECTION_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group();
            // 提取 hex 字符: §x§R§R§G§G§B§B -> RRGGBB
            StringBuilder hex = new StringBuilder();
            for (int i = 3; i < match.length(); i += 2) {
                hex.append(match.charAt(i));
            }
            matcher.appendReplacement(sb, "<color:#" + hex + ">");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String convertAmpersandHex(String text) {
        Matcher matcher = HEX_AMPERSAND_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(sb, "<color:#" + hex + ">");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String convertLegacyCodes(String text) {
        Matcher matcher = LEGACY_COLOR_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            char code = matcher.group(1).toLowerCase().charAt(0);
            String replacement = legacyToMiniMessage(code);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String legacyToMiniMessage(char code) {
        return switch (code) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> "";
        };
    }
}
