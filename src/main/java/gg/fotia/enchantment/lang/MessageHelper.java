package gg.fotia.enchantment.lang;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.util.MiniMessageCache;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息辅助工具
 * 负责消息发送、占位符替换和文本解析。
 * 最终文本解析走 MiniMessageCache, 相同消息内容不重复反序列化。
 */
public class MessageHelper {

    private final FotiaEnchantment plugin;
    private final LanguageManager languageManager;

    /** 已告警过的缺失消息键, 每个键只提示一次 */
    private final Set<String> warnedMissingKeys = ConcurrentHashMap.newKeySet();

    public MessageHelper(FotiaEnchantment plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
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
            // 缺键静默会让排查困难, 向控制台提示一次
            if (key != null && warnedMissingKeys.add(key)) {
                plugin.getLogger().warning("缺少消息键: " + key + " (请检查各语言的 messages.yml)");
            }
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

        // 解析 (兼容旧颜色码, 内容级缓存)
        return MiniMessageCache.deserializeLegacyAware(text);
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
        return MiniMessageCache.deserializeLegacyAware(text);
    }
}
