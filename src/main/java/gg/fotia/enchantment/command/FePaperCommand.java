package gg.fotia.enchantment.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

final class FePaperCommand implements BasicCommand {

    private final CommandManager commandManager;
    private final Command bridge;

    FePaperCommand(CommandManager commandManager, Command bridge) {
        this.commandManager = commandManager;
        this.bridge = bridge;
    }

    @Override
    public void execute(@NotNull CommandSourceStack source, @NotNull String[] args) {
        commandManager.onCommand(source.getSender(), bridge, "fe",
                CommandManager.normalizeArgsForSuggestions(args));
    }

    @Override
    public @NotNull List<String> suggest(@NotNull CommandSourceStack source,
                                         @NotNull String[] args) {
        List<String> result = commandManager.onTabComplete(source.getSender(), bridge, "fe",
                CommandManager.normalizeArgsForSuggestions(args));
        return result == null ? Collections.emptyList() : result;
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return true;
    }
}
