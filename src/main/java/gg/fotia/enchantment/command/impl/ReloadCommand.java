package gg.fotia.enchantment.command.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.command.SubCommand;
import gg.fotia.enchantment.config.EnchantmentConfig.ConfigIssue;
import gg.fotia.enchantment.core.EnchantmentManager.UndefinedConflict;
import gg.fotia.enchantment.lang.MessageHelper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /fe reload
 * 重载所有配置
 */
public class ReloadCommand implements SubCommand {

    private static final int MAX_WARNING_ENTRIES = 5;

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

        List<ConfigIssue> configIssues = new java.util.ArrayList<>(plugin.getConfigManager().getConfigIssues());
        configIssues.addAll(plugin.getEnchantmentManager().getConfigIssues());
        List<UndefinedConflict> undefinedConflicts = plugin.getEnchantmentManager().getUndefinedConflicts();
        if (sender instanceof Player player) {
            MessageHelper messageHelper = plugin.getMessageHelper();
            messageHelper.sendMessage(player, "reload-success");
            notifyConfigIssues(player, configIssues, messageHelper);
            notifyUndefinedConflicts(player, undefinedConflicts, messageHelper);
        } else {
            sender.sendMessage("FotiaEnchantment configuration reloaded.");
            notifyConfigIssues(sender, configIssues);
            notifyUndefinedConflicts(sender, undefinedConflicts);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    private void notifyUndefinedConflicts(Player player, List<UndefinedConflict> conflicts, MessageHelper messageHelper) {
        if (conflicts == null || conflicts.isEmpty()) {
            return;
        }

        messageHelper.sendMessage(player, "reload-undefined-conflicts-header",
                Map.of("count", String.valueOf(conflicts.size())));
        for (UndefinedConflict conflict : conflicts.stream().limit(MAX_WARNING_ENTRIES).toList()) {
            messageHelper.sendMessage(player, "reload-undefined-conflict-entry",
                    Map.of("source", conflict.sourceId(),
                            "conflict", conflict.conflictId(),
                            "file", conflict.sourceFile()));
        }
        int remaining = conflicts.size() - MAX_WARNING_ENTRIES;
        if (remaining > 0) {
            messageHelper.sendMessage(player, "reload-undefined-conflicts-more",
                    Map.of("count", String.valueOf(remaining)));
        }
    }

    private void notifyUndefinedConflicts(CommandSender sender, List<UndefinedConflict> conflicts) {
        if (conflicts == null || conflicts.isEmpty()) {
            return;
        }

        sender.sendMessage("FotiaEnchantment warning: found " + conflicts.size()
                + " undefined enchantment conflict reference(s).");
        for (UndefinedConflict conflict : conflicts.stream().limit(MAX_WARNING_ENTRIES).toList()) {
            sender.sendMessage("- file: " + conflict.sourceFile()
                    + " | path: conflicts | enchantment: " + conflict.sourceId()
                    + " -> undefined: " + conflict.conflictId());
        }
        int remaining = conflicts.size() - MAX_WARNING_ENTRIES;
        if (remaining > 0) {
            sender.sendMessage("... and " + remaining + " more. Check the server log for the full list.");
        }
    }

    private void notifyConfigIssues(Player player, List<ConfigIssue> issues, MessageHelper messageHelper) {
        if (issues == null || issues.isEmpty()) {
            return;
        }

        messageHelper.sendMessage(player, "reload-config-errors-header",
                Map.of("count", String.valueOf(issues.size())));
        for (ConfigIssue issue : issues.stream().limit(MAX_WARNING_ENTRIES).toList()) {
            messageHelper.sendMessage(player, "reload-config-error-entry",
                    Map.of("file", issue.filePath(),
                            "path", issue.path(),
                            "message", issue.message()));
        }
        int remaining = issues.size() - MAX_WARNING_ENTRIES;
        if (remaining > 0) {
            messageHelper.sendMessage(player, "reload-config-errors-more",
                    Map.of("count", String.valueOf(remaining)));
        }
    }

    private void notifyConfigIssues(CommandSender sender, List<ConfigIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return;
        }

        sender.sendMessage("FotiaEnchantment warning: found " + issues.size()
                + " configuration error(s). Related enchantments/configs were not loaded.");
        for (ConfigIssue issue : issues.stream().limit(MAX_WARNING_ENTRIES).toList()) {
            sender.sendMessage("- file: " + issue.filePath()
                    + " | path: " + issue.path()
                    + " | reason: " + issue.message());
        }
        int remaining = issues.size() - MAX_WARNING_ENTRIES;
        if (remaining > 0) {
            sender.sendMessage("... and " + remaining + " more. Check the server log for the full list.");
        }
    }
}
