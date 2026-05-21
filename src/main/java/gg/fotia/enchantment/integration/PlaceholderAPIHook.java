package gg.fotia.enchantment.integration;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.core.PDCManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.logging.Level;

/**
 * PlaceholderAPI 集成钩子
 * <p>
 * 注册 fotia 展开式, 提供以下变量:
 * <ul>
 *     <li>%fotia_enchant_count% - 主手物品自定义附魔数量</li>
 *     <li>%fotia_enchant_list% - 主手物品自定义附魔列表 (英文逗号分隔, 形如 "id:level")</li>
 *     <li>%fotia_enchant_has_&lt;id&gt;% - 是否拥有指定附魔 (yes/no)</li>
 *     <li>%fotia_enchant_level_&lt;id&gt;% - 指定附魔等级 (无则为 0)</li>
 * </ul>
 */
public class PlaceholderAPIHook {

    private final FotiaEnchantment plugin;
    private boolean available;
    private FotiaExpansion expansion;

    public PlaceholderAPIHook(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    /**
     * 注册展开式
     */
    public void register() {
        try {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
                available = false;
                return;
            }
            expansion = new FotiaExpansion(plugin);
            available = expansion.register();
            if (available) {
                plugin.getLogger().info("PlaceholderAPI 集成已启用 (展开式: fotia)");
            }
        } catch (Throwable t) {
            available = false;
            plugin.getLogger().log(Level.WARNING, "注册 PlaceholderAPI 展开式失败", t);
        }
    }

    /**
     * 注销展开式
     */
    public void unregister() {
        if (expansion != null) {
            try {
                expansion.unregister();
            } catch (Throwable ignored) {
            }
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Fotia 展开式实现
     */
    private static class FotiaExpansion extends PlaceholderExpansion {

        private final FotiaEnchantment plugin;

        FotiaExpansion(FotiaEnchantment plugin) {
            this.plugin = plugin;
        }

        @Override
        public String getIdentifier() {
            return "fotia";
        }

        @Override
        public String getAuthor() {
            return "Fotia";
        }

        @Override
        public String getVersion() {
            return plugin.getPluginMeta().getVersion();
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public String onRequest(OfflinePlayer offline, String params) {
            if (offline == null || params == null) {
                return "";
            }
            Player player = offline.getPlayer();
            if (player == null) {
                return "";
            }

            ItemStack hand = player.getInventory().getItemInMainHand();
            EnchantmentManager enchantManager = plugin.getEnchantmentManager();
            if (enchantManager == null) {
                return "";
            }
            PDCManager pdc = enchantManager.getPdcManager();
            Map<String, Integer> enchants = pdc.getEnchantments(hand);

            String lower = params.toLowerCase();

            if (lower.equals("enchant_count")) {
                return String.valueOf(enchants.size());
            }

            if (lower.equals("enchant_list")) {
                if (enchants.isEmpty()) return "";
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Integer> e : enchants.entrySet()) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(e.getKey()).append(":").append(e.getValue());
                }
                return sb.toString();
            }

            if (lower.startsWith("enchant_has_")) {
                String id = params.substring("enchant_has_".length());
                return enchants.containsKey(id) ? "yes" : "no";
            }

            if (lower.startsWith("enchant_level_")) {
                String id = params.substring("enchant_level_".length());
                return String.valueOf(enchants.getOrDefault(id, 0));
            }

            // 附魔元信息 (如 fotia_enchant_max_<id>)
            if (lower.startsWith("enchant_max_")) {
                String id = params.substring("enchant_max_".length());
                EnchantmentData data = enchantManager.getEnchantment(id);
                return data != null ? String.valueOf(data.getMaxLevel()) : "0";
            }

            return null;
        }
    }
}
