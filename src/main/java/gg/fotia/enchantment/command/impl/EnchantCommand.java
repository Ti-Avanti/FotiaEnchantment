package gg.fotia.enchantment.command.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.command.SubCommand;
import gg.fotia.enchantment.core.EnchantmentConflictPolicy;
import gg.fotia.enchantment.core.EnchantmentData;
import gg.fotia.enchantment.core.EnchantmentLimitPolicy;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.core.PDCManager;
import gg.fotia.enchantment.lang.MessageHelper;
import gg.fotia.enchantment.lore.item.EnchantmentLoreCleaner;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /fe enchant <enchant_id> <level>
 * 给自己手持物品附魔
 */
public class EnchantCommand implements SubCommand {

    private final FotiaEnchantment plugin;

    public EnchantCommand(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "enchant";
    }

    @Override
    public String getPermission() {
        return "fotia.enchantment.enchant";
    }

    @Override
    public String getUsage() {
        return "/fe enchant <enchant_id> <level>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        MessageHelper messageHelper = plugin.getMessageHelper();
        EnchantmentManager enchantmentManager = plugin.getEnchantmentManager();
        PDCManager pdcManager = enchantmentManager.getPdcManager();

        if (args.length < 2) {
            sender.sendMessage("§c用法: " + getUsage());
            return;
        }

        // 查找附魔
        String enchantId = args[0].toLowerCase();
        EnchantmentData data = enchantmentManager.getEnchantment(enchantId);
        if (data == null) {
            messageHelper.sendMessage(player, "enchant-not-found", Map.of("enchant_id", enchantId));
            return;
        }

        // 解析等级
        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            messageHelper.sendMessage(player, "invalid-level", Map.of("max_level", String.valueOf(data.getMaxLevel())));
            return;
        }

        if (level < 1 || level > data.getMaxLevel()) {
            messageHelper.sendMessage(player, "invalid-level", Map.of("max_level", String.valueOf(data.getMaxLevel())));
            return;
        }

        // 检查手持物品
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            messageHelper.sendMessage(player, "not-holding-item");
            return;
        }

        // 检查适用性
        if (!pdcManager.isApplicable(item, data)) {
            messageHelper.sendMessage(player, "enchant-not-applicable");
            return;
        }

        // 检查冲突
        if (pdcManager.hasConflict(item, data, enchantmentManager::getEnchantment)) {
            // 找到具体冲突的附魔名称
            String conflictName = findConflictName(player, item, data);
            messageHelper.sendMessage(player, "enchant-conflict", Map.of("conflict_name", conflictName));
            return;
        }

        int max = plugin.getConfigManager().getMaxEnchantmentsForMaterial(item.getType());
        if (!EnchantmentLimitPolicy.canAddEnchantment(item, pdcManager, enchantId, max)) {
            messageHelper.sendMessage(player, "max-enchants-reached", Map.of("max", String.valueOf(max)));
            return;
        }

        // 添加附魔
        EnchantmentLoreCleaner.stripGeneratedLore(plugin, player, item);
        pdcManager.addEnchantment(item, enchantId, level);
        EnchantmentLoreCleaner.applyGeneratedLore(plugin, player, item);
        player.getInventory().setItemInMainHand(item);
        player.updateInventory();

        // 获取附魔显示名称
        String enchantName = plugin.getLanguageManager().getEnchantName(player, enchantId);
        messageHelper.sendMessage(player, "enchant-applied", Map.of(
                "enchant_name", enchantName,
                "level", String.valueOf(level)
        ));
    }

    /**
     * 查找与指定附魔冲突的现有附魔名称
     */
    private String findConflictName(Player player, ItemStack item, EnchantmentData data) {
        PDCManager pdcManager = plugin.getEnchantmentManager().getPdcManager();
        Map<String, Integer> existing = pdcManager.getEnchantments(item);
        EnchantmentManager enchantmentManager = plugin.getEnchantmentManager();
        for (String existingId : existing.keySet()) {
            if (EnchantmentConflictPolicy.hasCustomConflict(
                    data.getId(),
                    data,
                    java.util.Set.of(existingId),
                    enchantmentManager::getEnchantment)) {
                return plugin.getLanguageManager().getEnchantName(
                        player,
                        EnchantmentConflictPolicy.normalizeCustomId(existingId));
            }
        }
        return "unknown";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 补全附魔ID
            String input = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (EnchantmentData data : plugin.getEnchantmentManager().getAllEnchantments()) {
                if (data.getId().startsWith(input)) {
                    completions.add(data.getId());
                }
            }
            return completions;
        }

        if (args.length == 2) {
            // 补全等级
            String enchantId = args[0].toLowerCase();
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
