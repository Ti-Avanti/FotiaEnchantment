package gg.fotia.enchantment.command.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.command.SubCommand;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.EnchantmentLimitPolicy;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.lang.MessageHelper;
import gg.fotia.enchantment.lore.item.EnchantmentLoreCleaner;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /fe give <player> <enchant_id> <level>
 * 给玩家手持物品添加指定附魔
 */
public class GiveCommand implements SubCommand {

    private final FotiaEnchantment plugin;

    public GiveCommand(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "give";
    }

    @Override
    public String getPermission() {
        return "fotia.enchantment.give";
    }

    @Override
    public String getUsage() {
        return "/fe give <player> <enchant_id> <level>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        MessageHelper messageHelper = plugin.getMessageHelper();
        EnchantmentManager enchantmentManager = plugin.getEnchantmentManager();
        PDCManager pdcManager = enchantmentManager.getPdcManager();

        if (args.length < 3) {
            sender.sendMessage("§c用法: " + getUsage());
            return;
        }

        // 查找目标玩家
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            if (sender instanceof Player player) {
                messageHelper.sendMessage(player, "player-not-found", Map.of("player", args[0]));
            } else {
                sender.sendMessage("Player " + args[0] + " not found.");
            }
            return;
        }

        // 查找附魔
        String enchantId = args[1].toLowerCase();
        EnchantmentData data = enchantmentManager.getEnchantment(enchantId);
        if (data == null) {
            if (sender instanceof Player player) {
                messageHelper.sendMessage(player, "enchant-not-found", Map.of("enchant_id", enchantId));
            } else {
                sender.sendMessage("Enchantment " + enchantId + " not found.");
            }
            return;
        }

        // 解析等级
        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            if (sender instanceof Player player) {
                messageHelper.sendMessage(player, "invalid-level", Map.of("max_level", String.valueOf(data.getMaxLevel())));
            } else {
                sender.sendMessage("Invalid level. Max level: " + data.getMaxLevel());
            }
            return;
        }

        if (level < 1 || level > data.getMaxLevel()) {
            if (sender instanceof Player player) {
                messageHelper.sendMessage(player, "invalid-level", Map.of("max_level", String.valueOf(data.getMaxLevel())));
            } else {
                sender.sendMessage("Invalid level. Max level: " + data.getMaxLevel());
            }
            return;
        }

        // 检查目标手持物品
        ItemStack item = target.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            if (sender instanceof Player player) {
                messageHelper.sendMessage(player, "not-holding-item");
            } else {
                sender.sendMessage("Target player is not holding an item.");
            }
            return;
        }

        // 检查适用性
        if (!pdcManager.isApplicable(item, data)) {
            if (sender instanceof Player player) {
                messageHelper.sendMessage(player, "enchant-not-applicable");
            } else {
                sender.sendMessage("This enchantment cannot be applied to that item.");
            }
            return;
        }

        // 检查冲突
        if (pdcManager.hasConflict(item, data)) {
            if (sender instanceof Player player) {
                messageHelper.sendMessage(player, "enchant-conflict", Map.of("conflict_name", enchantId));
            } else {
                sender.sendMessage("This enchantment conflicts with existing enchantments.");
            }
            return;
        }

        int max = plugin.getConfigManager().getMaxEnchantmentsForMaterial(item.getType());
        if (!EnchantmentLimitPolicy.canAddEnchantment(item, pdcManager, enchantId, max)) {
            if (sender instanceof Player player) {
                messageHelper.sendMessage(player, "max-enchants-reached", Map.of("max", String.valueOf(max)));
            } else {
                sender.sendMessage("Target item has reached the maximum number of enchantments (" + max + ").");
            }
            return;
        }

        // 添加附魔
        EnchantmentLoreCleaner.stripGeneratedLore(plugin, target, item);
        pdcManager.addEnchantment(item, enchantId, level);
        target.getInventory().setItemInMainHand(item);

        // 获取附魔显示名称
        String enchantName = plugin.getLanguageManager().getEnchantName(
                sender instanceof Player p ? p : target, enchantId);

        if (sender instanceof Player player) {
            messageHelper.sendMessage(player, "give-success", Map.of(
                    "player", target.getName(),
                    "enchant_name", enchantName,
                    "level", String.valueOf(level)
            ));
        } else {
            sender.sendMessage("Gave " + target.getName() + " enchantment: " + enchantName + " " + level);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 补全玩家名
            String input = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
            return completions;
        }

        if (args.length == 2) {
            // 补全附魔ID
            String input = args[1].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (EnchantmentData data : plugin.getEnchantmentManager().getAllEnchantments()) {
                if (data.getId().startsWith(input)) {
                    completions.add(data.getId());
                }
            }
            return completions;
        }

        if (args.length == 3) {
            // 补全等级
            String enchantId = args[1].toLowerCase();
            EnchantmentData data = plugin.getEnchantmentManager().getEnchantment(enchantId);
            if (data != null) {
                List<String> completions = new ArrayList<>();
                for (int i = 1; i <= data.getMaxLevel(); i++) {
                    completions.add(String.valueOf(i));
                }
                return completions;
            }
        }

        return Collections.emptyList();
    }
}
