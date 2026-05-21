package gg.fotia.enchantment.command.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.command.SubCommand;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.lang.MessageHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * /fe list [category] [page]
 * 列出所有附魔（可按类别筛选），分页显示
 */
public class ListCommand implements SubCommand {

    private static final int PAGE_SIZE = 10;
    private static final List<String> CATEGORIES = Arrays.asList(
            "melee", "ranged", "armor", "tools", "universal"
    );

    private final FotiaEnchantment plugin;

    public ListCommand(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getPermission() {
        return "fotia.enchantment.list";
    }

    @Override
    public String getUsage() {
        return "/fe list [category] [page]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        MessageHelper messageHelper = plugin.getMessageHelper();

        // 解析参数
        String category = null;
        int page = 1;

        if (args.length >= 1) {
            // 判断第一个参数是类别还是页码
            if (isNumber(args[0])) {
                page = Integer.parseInt(args[0]);
            } else {
                category = args[0].toLowerCase();
            }
        }
        if (args.length >= 2) {
            if (isNumber(args[1])) {
                page = Integer.parseInt(args[1]);
            }
        }

        // 获取附魔列表
        List<EnchantmentData> enchantments;
        if (category != null) {
            enchantments = plugin.getEnchantmentManager().getByCategory(category);
        } else {
            enchantments = new ArrayList<>(plugin.getEnchantmentManager().getAllEnchantments());
        }

        if (enchantments.isEmpty()) {
            messageHelper.sendMessage(player, "list-empty");
            return;
        }

        // 计算分页
        int totalPages = (int) Math.ceil((double) enchantments.size() / PAGE_SIZE);
        page = Math.max(1, Math.min(page, totalPages));

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, enchantments.size());

        // 发送页头
        messageHelper.sendMessage(player, "list-header", Map.of(
                "current", String.valueOf(page),
                "total", String.valueOf(totalPages)
        ));

        // 获取稀有度配置
        YamlConfiguration rarityConfig = plugin.getConfigManager().getRarityConfig();

        // 发送每个附魔的条目
        for (int i = start; i < end; i++) {
            EnchantmentData data = enchantments.get(i);
            String enchantName = plugin.getLanguageManager().getEnchantName(player, data.getId());
            String rarityColor = getRarityColor(rarityConfig, data.getRarity());
            String rarityName = getRarityName(player, data.getRarity());

            Component entryComponent = messageHelper.parseText(player,
                    plugin.getLanguageManager().getMessage(player, "list-entry"),
                    Map.of(
                            "enchant_name", enchantName,
                            "rarity_color", rarityColor,
                            "rarity_name", rarityName,
                            "max_level", String.valueOf(data.getMaxLevel())
                    ));
            player.sendMessage(entryComponent);
        }
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

    private boolean isNumber(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (String cat : CATEGORIES) {
                if (cat.startsWith(input)) {
                    completions.add(cat);
                }
            }
            return completions;
        }

        return Collections.emptyList();
    }
}
