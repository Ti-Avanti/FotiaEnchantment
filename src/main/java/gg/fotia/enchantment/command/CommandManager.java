package gg.fotia.enchantment.command;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.command.impl.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 命令管理器
 * 注册 /fe 主命令，路由子命令，处理Tab补全
 */
public class CommandManager implements CommandExecutor, TabCompleter {

    private final FotiaEnchantment plugin;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public CommandManager(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化命令系统：注册所有子命令并绑定到Bukkit
     */
    public void init() {
        // 注册子命令
        register(new GiveCommand(plugin));
        register(new EnchantCommand(plugin));
        register(new RemoveCommand(plugin));
        register(new ListCommand(plugin));
        register(new InfoCommand(plugin));
        register(new ReloadCommand(plugin));
        register(new GUICommand(plugin));
        register(new GiveItemCommand(plugin));
        registerPermissions();

        // 注册到Bukkit
        try {
            PluginCommand cmd = plugin.getCommand("fe");
            if (cmd != null) {
                cmd.setExecutor(this);
                cmd.setTabCompleter(this);
            } else {
                registerPaperCommand();
            }
        } catch (UnsupportedOperationException ex) {
            registerPaperCommand();
        }
    }

    /**
     * 注册子命令
     */
    private void register(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    private boolean registerDynamicCommand() {
        return plugin.getServer().getCommandMap()
                .register(plugin.getName().toLowerCase(Locale.ROOT), new FeBukkitCommand(this));
    }

    private void registerPaperCommand() {
        try {
            PaperCommandRegistrar.register(plugin, this, this::fallbackToDynamicCommand);
        } catch (RuntimeException | LinkageError ex) {
            fallbackToDynamicCommand(ex);
        }
    }

    private void fallbackToDynamicCommand(Throwable cause) {
        boolean legacyRegistered = registerDynamicCommand();
        if (!legacyRegistered) {
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof LinkageError linkageError) {
                throw linkageError;
            }
            throw new IllegalStateException(cause);
        }
        plugin.getLogger().warning("Paper command registration failed; using Bukkit command fallback. Cause: "
                + cause.getClass().getSimpleName() + ": " + String.valueOf(cause.getMessage()));
    }

    static String[] normalizeArgsForSuggestions(String[] args) {
        if (args.length != 1 || !args[0].contains(" ")) {
            return args;
        }
        String raw = args[0].stripLeading();
        if (raw.isEmpty()) {
            return new String[]{""};
        }
        return raw.split("\\s+", -1);
    }

    private void registerPermissions() {
        registerPermission("fotia.enchantment.use", PermissionDefault.TRUE);
        registerPermission("fotia.enchantment.list", PermissionDefault.TRUE);
        registerPermission("fotia.enchantment.info", PermissionDefault.TRUE);
        registerPermission("fotia.enchantment.give", PermissionDefault.OP);
        registerPermission("fotia.enchantment.enchant", PermissionDefault.OP);
        registerPermission("fotia.enchantment.remove", PermissionDefault.OP);
        registerPermission("fotia.enchantment.reload", PermissionDefault.OP);
        registerPermission("fotia.enchantment.gui", PermissionDefault.OP);
        registerPermission("fotia.enchantment.giveitem", PermissionDefault.OP);
        registerPermission("fotia.enchantment.update", PermissionDefault.OP);
    }

    private void registerPermission(String node, PermissionDefault permissionDefault) {
        if (plugin.getServer().getPluginManager().getPermission(node) == null) {
            plugin.getServer().getPluginManager().addPermission(new Permission(node, permissionDefault));
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subName = args[0].toLowerCase();
        SubCommand sub = subCommands.get(subName);
        if (sub == null) {
            sendHelp(sender);
            return true;
        }

        // 权限检查
        if (sub.getPermission() != null && !sender.hasPermission(sub.getPermission())) {
            if (sender instanceof Player player) {
                plugin.getMessageHelper().sendMessage(player, "no-permission");
            } else {
                sender.sendMessage("You do not have permission to do this.");
            }
            return true;
        }

        // 去除子命令名，传递剩余参数
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);
        sub.execute(sender, subArgs);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            return subCommandCompletions(sender, "");
        }

        if (args.length == 1) {
            return subCommandCompletions(sender, args[0].toLowerCase(Locale.ROOT));
        }

        if (args.length >= 2) {
            String subName = args[0].toLowerCase();
            SubCommand sub = subCommands.get(subName);
            if (sub != null) {
                // 权限检查
                if (sub.getPermission() != null && !sender.hasPermission(sub.getPermission())) {
                    return Collections.emptyList();
                }
                // 去除子命令名，传递剩余参数
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, subArgs.length);
                return sub.tabComplete(sender, subArgs);
            }
        }

        return Collections.emptyList();
    }

    private List<String> subCommandCompletions(CommandSender sender, String input) {
        List<String> completions = new ArrayList<>();
        for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
            if (entry.getKey().startsWith(input)) {
                // 只显示有权限的子命令
                if (entry.getValue().getPermission() == null
                        || sender.hasPermission(entry.getValue().getPermission())) {
                    completions.add(entry.getKey());
                }
            }
        }
        return completions;
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        if (sender instanceof Player player) {
            plugin.getMessageHelper().sendMessage(player, "command-help");
        } else {
            sender.sendMessage("FotiaEnchantment Commands:");
            sender.sendMessage("/fe give <player> <enchant_id> <level> - Give enchantment");
            sender.sendMessage("/fe enchant <enchant_id> <level> - Enchant held item");
            sender.sendMessage("/fe remove <enchant_id> - Remove enchantment");
            sender.sendMessage("/fe list [category] [page] - List enchantments");
            sender.sendMessage("/fe info <enchant_id> - View enchantment info");
            sender.sendMessage("/fe reload - Reload config");
            sender.sendMessage("/fe gui [guide|admin] - Open encyclopedia/admin GUI");
            sender.sendMessage("/fe giveitem <player> <item_type> [amount] [rarity] - Give custom item");
        }
    }
}
