package gg.fotia.enchantment.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MiniMessage 解析缓存。
 * <p>
 * lore 改写与消息渲染会对相同文本反复执行 LegacyColorConverter + MiniMessage.deserialize,
 * 这里按原始文本缓存解析结果。Component 不可变, 可跨线程安全共享;
 * 缓存以内容为键, 配置/语言重载后新文本自然产生新条目, 无需显式失效。
 * 超过上限整体清空, 防止无界增长。
 */
public final class MiniMessageCache {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final int CACHE_MAX = 2048;
    private static final Map<String, Component> CACHE = new ConcurrentHashMap<>();

    private MiniMessageCache() {
    }

    /**
     * 解析文本 (兼容 &/§ 旧颜色码), 结果缓存
     */
    public static Component deserializeLegacyAware(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        Component cached = CACHE.get(text);
        if (cached != null) {
            return cached;
        }
        Component parsed = MINI.deserialize(LegacyColorConverter.convert(text));
        if (CACHE.size() >= CACHE_MAX) {
            CACHE.clear();
        }
        CACHE.put(text, parsed);
        return parsed;
    }
}
