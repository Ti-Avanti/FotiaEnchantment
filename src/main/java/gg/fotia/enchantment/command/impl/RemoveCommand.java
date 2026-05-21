package gg.fotia.enchantment.command.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.command.SubCommand;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.lang.MessageHelper;
import gg.fotia.enchantment.lore.EnchantmentLoreCleaner;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /fe remove <enchant_id>
 * 从手持物品移除指定附魔
 */
public class RemoveCommand implements SubCommand {

    private final FotiaEnchantment plugin;

    public RemoveCommand(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getPermission() {
        return "fotia.enchantment.remove";
    }

    @Override
    public String getUsage() {
        return "/fe remove <enchant_id>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        MessageHelper messageHelper = plugin.getMessageHelper();
        PDCManager pdcManager = plugin.getEnchantmentManager().getPdcManager();

        if (args.length < 1) {
            sender.sendMessage("§c用法: " + getUsage());
            return;
        }

        // 检查手持物品
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            messageHelper.sendMessage(player, "not-holding-item");
            return;
        }

        // 查找附魔
        String enchantId = args[0].toLowerCase();
        EnchantmentData data = plugin.getEnchantmentManager().getEnchantment(enchantId);
        if (data == null) {
            messageHelper.sendMessage(player, "enchant-not-found", Map.of("enchant_id", enchantId));
            return;
        }

        // 检查物品上是否有该附魔
        if (!pdcManager.hasEnchantment(item, enchantId)) {
            messageHelper.sendMessage(player, "enchant-not-found", Map.of("enchant_id", enchantId));
            return;
        }

        // 移除附魔
        EnchantmentLoreCleaner.stripGeneratedLore(plugin, player, item);
        pdcManager.removeEnchantment(item, enchantId);
        player.getInventory().setItemInMainHand(item);

        // 获取附魔显示名称
        String enchantName = plugin.getLanguageManager().getEnchantName(player, enchantId);
        messageHelper.sendMessage(player, "enchant-removed", Map.of("enchant_name", enchantName));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 补全当前物品上的附魔ID
            if (sender instanceof Player player) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (!item.getType().isAir()) {
                    PDCManager pdcManager = plugin.getEnchantmentManager().getPdcManager();
                    Map<String, Integer> enchants = pdcManager.getEnchantments(item);
                    String input = args[0].toLowerCase();
                    List<String> completions = new ArrayList<>();
                    for (String enchantId : enchants.keySet()) {
                        if (enchantId.startsWith(input)) {
                            completions.add(enchantId);
                        }
                    }
                    return completions;
                }
            }
            // 如果不是玩家或没有物品，返回所有附魔ID
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
