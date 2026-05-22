package gg.fotia.enchantment.command.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.command.SubCommand;
import gg.fotia.enchantment.gui.admin.AdminGUI;
import gg.fotia.enchantment.gui.guide.EnchantmentGuideGUI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * /fe gui [guide|admin]
 */
public class GUICommand implements SubCommand {

    private static final String ADMIN_PERMISSION = "fotia.enchantment.gui";

    private final FotiaEnchantment plugin;

    public GUICommand(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "gui";
    }

    @Override
    public String getPermission() {
        return "fotia.enchantment.use";
    }

    @Override
    public String getUsage() {
        return "/fe gui [guide|admin]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        String mode = args.length == 0 ? "guide" : args[0].toLowerCase(Locale.ROOT);
        if ("admin".equals(mode)) {
            if (!player.hasPermission(ADMIN_PERMISSION)) {
                plugin.getMessageHelper().sendMessage(player, "no-permission");
                return;
            }
            plugin.getGuiManager().open(new AdminGUI(plugin, player));
            return;
        }

        plugin.getGuiManager().open(new EnchantmentGuideGUI(plugin, player));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            List<String> completions = new ArrayList<>();
            if ("guide".startsWith(input)) {
                completions.add("guide");
            }
            if ("admin".startsWith(input) && sender != null && sender.hasPermission(ADMIN_PERMISSION)) {
                completions.add("admin");
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
