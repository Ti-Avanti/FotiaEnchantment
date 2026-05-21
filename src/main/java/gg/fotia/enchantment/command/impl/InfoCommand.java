package gg.fotia.enchantment.command.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.command.SubCommand;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.lang.MessageHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * /fe info <enchant_id>
 * 显示附魔详细信息
 */
public class InfoCommand implements SubCommand {

    private final FotiaEnchantment plugin;

    public InfoCommand(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getPermission() {
        return "fotia.enchantment.info";
    }

    @Override
    public String getUsage() {
        return "/fe info <enchant_id>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        MessageHelper messageHelper = plugin.getMessageHelper();

        if (args.length < 1) {
            sender.sendMessage("§c用法: " + getUsage());
            return;
        }

        // 查找附魔
        String enchantId = args[0].toLowerCase();
        EnchantmentData data = plugin.getEnchantmentManager().getEnchantment(enchantId);
        if (data == null) {
            messageHelper.sendMessage(player, "enchant-not-found", Map.of("enchant_id", enchantId));
            return;
        }

        // 获取稀有度信息
        YamlConfiguration rarityConfig = plugin.getConfigManager().getRarityConfig();
        String rarityColor = getRarityColor(rarityConfig, data.getRarity());
        String rarityName = getRarityName(player, data.getRarity());
        String enchantName = plugin.getLanguageManager().getEnchantName(player, enchantId);

        // 发送信息头
        messageHelper.sendMessage(player, "info-header", Map.of(
                "enchant_name", enchantName,
                "rarity_color", rarityColor
        ));

        // 最大等级
        sendInfoLine(player, "info-level", Map.of("max_level", String.valueOf(data.getMaxLevel())));

        // 稀有度
        sendInfoLine(player, "info-rarity", Map.of(
                "rarity_color", rarityColor,
                "rarity_name", rarityName
        ));

        // 分组
        String group = data.getGroup() != null ? data.getGroup() : "none";
        sendInfoLine(player, "info-group", Map.of("group", group));

        // 适用物品
        String items = formatApplicableItems(data);
        sendInfoLine(player, "info-applicable", Map.of("items", items));

        // 冲突附魔
        String conflicts = formatConflicts(player, data);
        sendInfoLine(player, "info-conflicts", Map.of("conflicts", conflicts));
    }

    /**
     * 发送信息行
     */
    private void sendInfoLine(Player player, String key, Map<String, String> placeholders) {
        MessageHelper messageHelper = plugin.getMessageHelper();
        String raw = plugin.getLanguageManager().getMessage(player, key);
        Component component = messageHelper.parseText(player, raw, placeholders);
        player.sendMessage(component);
    }

    /**
     * 格式化适用物品列表
     */
    private String formatApplicableItems(EnchantmentData data) {
        List<Material> items = data.getApplicableItems();
        if (items == null || items.isEmpty()) {
            return "ALL";
        }
        return items.stream()
                .map(m -> m.name().toLowerCase())
                .collect(Collectors.joining(", "));
    }

    /**
     * 格式化冲突附魔列表
     */
    private String formatConflicts(Player player, EnchantmentData data) {
        List<String> conflicts = data.getConflicts();
        if (conflicts == null || conflicts.isEmpty()) {
            return "none";
        }
        return conflicts.stream()
                .map(id -> plugin.getLanguageManager().getEnchantName(player, id))
                .collect(Collectors.joining(", "));
    }

    /**
     * 获取稀有度颜色标签
     */
    private String getRarityColor(YamlConfiguration rarityConfig, String rarity) {
        if (rarity == null || rarityConfig == null) {
            return "<white>";
        }
        String color = rarityConfig.getString(rarity + ".color");
        return color != null ? color : "<white>";
    }

    /**
     * 获取稀有度本地化名称
     */
    private String getRarityName(Player player, String rarity) {
        if (rarity == null) {
            return "unknown";
        }
        String msg = plugin.getLanguageManager().getMessage(player, "rarity-" + rarity);
        return msg.equals("rarity-" + rarity) ? rarity : msg;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (EnchantmentData data : plugin.getEnchantmentManager().getAllEnchantments()) {
                if (data.getId().startsWith(input)) {
                    completions.add(data.getId());
                }
            }
            return completions;
        }

        return Collections.emptyList();
    }
}
