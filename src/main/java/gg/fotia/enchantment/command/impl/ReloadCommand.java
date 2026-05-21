package gg.fotia.enchantment.command.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.command.SubCommand;
import gg.fotia.enchantment.lang.MessageHelper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * /fe reload
 * 重载所有配置
 */
public class ReloadCommand implements SubCommand {

    private final FotiaEnchantment plugin;

    public ReloadCommand(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getPermission() {
        return "fotia.enchantment.reload";
    }

    @Override
    public String getUsage() {
        return "/fe reload";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // 重载配置管理器
        plugin.getConfigManager().reload();

        // 重载语言管理器
        plugin.getLanguageManager().reload();

        // 重载附魔管理器
        plugin.getEnchantmentManager().reload();

        // 重载原版附魔覆盖与效果管道运行参数
        plugin.getVanillaManager().reload();
        plugin.getEffectPipeline().reload();

        if (sender instanceof Player player) {
            plugin.getMessageHelper().sendMessage(player, "reload-success");
        } else {
            sender.sendMessage("FotiaEnchantment configuration reloaded.");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
