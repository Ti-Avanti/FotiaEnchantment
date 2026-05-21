package gg.fotia.enchantment.integration;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CraftEngine 集成钩子
 * 提供通过 CraftEngine 创建自定义物品的能力
 * 以及处理 {@code <image:namespace:key>} 标签
 */
public class CraftEngineHook {

    private final Logger logger;
    private boolean available;

    public CraftEngineHook(Logger logger) {
        this.logger = logger;
        Plugin craftEngine = Bukkit.getPluginManager().getPlugin("CraftEngine");
        this.available = craftEngine != null && craftEngine.isEnabled();
        if (available) {
            logger.info("已检测到 CraftEngine，集成已启用");
        }
    }

    /**
     * 检查 CraftEngine 是否可用
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * 使用 CraftEngine 创建物品
     *
     * @param craftEngineId CraftEngine 物品ID（格式: namespace:key）
     * @return 创建的物品，失败或不可用时返回 null
     */
    public ItemStack createItem(String craftEngineId) {
        return createItem(craftEngineId, 1, null);
    }

    /**
     * 使用 CraftEngine 创建指定数量的物品
     *
     * @param craftEngineId CraftEngine 物品ID（格式: namespace:key）
     * @param amount        数量
     * @param player        玩家上下文，可为空
     * @return 创建的物品，失败或不可用时返回 null
     */
    public ItemStack createItem(String craftEngineId, int amount, Player player) {
        if (!available || craftEngineId == null || craftEngineId.isEmpty()) {
            return null;
        }
        try {
            BukkitItemDefinition definition = CraftEngineItems.byId(craftEngineId);
            if (definition == null) {
                return null;
            }
            ItemStack item = player != null ? definition.buildBukkitItem(player) : definition.buildBukkitItem();
            item.setAmount(Math.max(1, amount));
            return item;
        } catch (LinkageError | RuntimeException e) {
            available = false;
            logger.log(Level.WARNING, "通过 CraftEngine 创建物品失败: " + craftEngineId, e);
            return null;
        }
    }

    /**
     * 处理文本中的 CraftEngine image 标签
     * 格式: {@code <image:namespace:key>}
     *
     * @param text 含有 image 标签的文本
     * @return 处理后的文本，CraftEngine 不可用时原样返回
     */
    public String processImageTags(String text) {
        // CraftEngine 的 image 标签由其文本渲染链处理，这里保留统一入口。
        return text;
    }
}
