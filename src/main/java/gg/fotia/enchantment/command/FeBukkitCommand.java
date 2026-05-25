package gg.fotia.enchantment.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

final class FeBukkitCommand extends Command {

    private final CommandManager commandManager;

    FeBukkitCommand(CommandManager commandManager) {
        super("fe", "FotiaEnchantment command", "/fe <subcommand>", List.of("fotia", "fotiaenchant"));
        this.commandManager = commandManager;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel,
                           @NotNull String[] args) {
        return commandManager.onCommand(sender, this, commandLabel, args);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender,
                                             @NotNull String alias,
                                             @NotNull String[] args) throws IllegalArgumentException {
        List<String> result = commandManager.onTabComplete(sender, this, alias, args);
        return result == null ? Collections.emptyList() : result;
    }
}
