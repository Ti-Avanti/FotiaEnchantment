package gg.fotia.enchantment.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.util.LegacyColorConverter;

import java.util.Collections;
import java.util.Map;

/**
 * 消息辅助工具
 * 负责消息发送、占位符替换和文本解析
 */
public class MessageHelper {

    private final FotiaEnchantment plugin;
    private final LanguageManager languageManager;
    private final MiniMessage miniMessage;

    public MessageHelper(FotiaEnchantment plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        this.miniMessage = MiniMessage.miniMessage();
    }

    /**
     * 发送翻译消息给玩家（带占位符）
     *
     * @param player       目标玩家
     * @param key          消息键
     * @param placeholders 占位符映射
     */
    public void sendMessage(Player player, String key, Map<String, String> placeholders) {
        String message = languageManager.getMessage(player, key);
        if (message == null || message.equals(key)) {
            return;
        }
        Component component = parseText(player, message, placeholders);
        player.sendMessage(component);
    }

    /**
     * 发送简单消息给玩家（无占位符）
     *
     * @param player 目标玩家
     * @param key    消息键
     */
    public void sendMessage(Player player, String key) {
        sendMessage(player, key, Collections.emptyMap());
    }

    /**
     * 解析文本为 Component（带占位符替换，自动处理旧颜色码）
     *
     * @param player       玩家（用于获取前缀等语言相关内容）
     * @param text         原始文本
     * @param placeholders 占位符映射
     * @return 解析后的 Component
     */
    public Component parseText(Player player, String text, Map<String, String> placeholders) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // 替换 {prefix} 占位符
        if (text.contains("{prefix}")) {
            String prefix = languageManager.getMessage(player, "prefix");
            if (prefix != null && !prefix.equals("prefix")) {
                text = text.replace("{prefix}", prefix);
            } else {
                text = text.replace("{prefix}", "");
            }
        }

        // 替换自定义占位符
        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                text = text.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
            }
        }

        // 先转换旧颜色码为 MiniMessage 格式
        text = LegacyColorConverter.convert(text);

        // 使用 MiniMessage 解析
        return miniMessage.deserialize(text);
    }

    /**
     * 直接解析文本为 Component（不做占位符替换，仅处理颜色码）
     *
     * @param text 原始文本
     * @return 解析后的 Component
     */
    public Component parseText(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // 转换旧颜色码
        text = LegacyColorConverter.convert(text);

        // 使用 MiniMessage 解析
        return miniMessage.deserialize(text);
    }
}
