package gg.fotia.enchantment.command.impl;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.command.SubCommand;
import gg.fotia.enchantment.item.CustomItemManager;
import gg.fotia.enchantment.lang.MessageHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GiveItemCommand implements SubCommand {

    private static final List<String> RARITIES = Arrays.asList(
            "dustlight", "moonlit", "radiant", "aureate", "divine"
    );

    private final FotiaEnchantment plugin;

    public GiveItemCommand(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "giveitem";
    }

    @Override
    public String getPermission() {
        return "fotia.enchantment.giveitem";
    }

    @Override
    public String getUsage() {
        return "/fe giveitem <player> <item_type> [amount] [rarity]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        MessageHelper messageHelper = plugin.getMessageHelper();

        if (args.length < 2) {
            sender.sendMessage("§c用法: " + getUsage());
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            if (sender instanceof Player player) {
                messageHelper.sendMessage(player, "player-not-found", Map.of("player", args[0]));
            } else {
                sender.sendMessage("Player " + args[0] + " not found.");
            }
            return;
        }

        String itemType = args[1].toLowerCase();
        if (!itemTypes().contains(itemType)) {
            if (sender instanceof Player player) {
                messageHelper.sendMessage(player, "item-not-found", Map.of("item_id", itemType));
            } else {
                sender.sendMessage("Item type " + itemType + " not found.");
            }
            return;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1) {
                    amount = 1;
                }
                if (amount > 64) {
                    amount = 64;
                }
            } catch (NumberFormatException e) {
                amount = 1;
            }
        }

        String rarity = null;
        if (args.length >= 4) {
            rarity = args[3].toLowerCase();
            if (!RARITIES.contains(rarity)) {
                rarity = "dustlight";
            }
        }

        ItemStack item = createItem(target, itemType, rarity, amount);
        if (item == null) {
            if (sender instanceof Player player) {
                messageHelper.sendMessage(player, "item-not-found", Map.of("item_id", itemType));
            } else {
                sender.sendMessage("Item type " + itemType + " not found.");
            }
            return;
        }

        giveItem(target, item);
        String itemName = getItemDisplayName(target, itemType, rarity);

        if (sender instanceof Player player) {
            messageHelper.sendMessage(player, "give-item-success", Map.of(
                    "player", target.getName(),
                    "item_name", itemName,
                    "amount", String.valueOf(amount)
            ));
        } else {
            sender.sendMessage("Gave " + target.getName() + " item: " + itemName + " x" + amount);
        }
    }

    private ItemStack createItem(Player player, String itemType, String rarity, int amount) {
        CustomItemManager itemManager = plugin.getCustomItemManager();
        ItemStack item;
        if ("fragment".equals(itemType)) {
            item = itemManager.createStarweaveFragment(player, amount);
        } else if ("codex".equals(itemType)) {
            item = itemManager.createStellarisCodex(player, rarity != null ? rarity : "dustlight");
        } else if (itemManager.isDisenchantItemType(itemType)) {
            item = itemManager.createDisenchantStone(player, itemType);
        } else if ("anvil-breakthrough-stone".equals(itemType)) {
            item = itemManager.createAnvilBreakthroughStone(player, amount);
        } else {
            item = null;
        }
        if (item != null) {
            item.setAmount(amount);
        }
        return item;
    }

    private void giveItem(Player player, ItemStack item) {
        var leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    private String getItemDisplayName(Player player, String itemType, String rarity) {
        CustomItemManager itemManager = plugin.getCustomItemManager();
        if ("fragment".equals(itemType)) {
            return plugin.getLanguageManager().getItemName(player, "starweave-fragment");
        }
        if ("codex".equals(itemType)) {
            String r = rarity != null ? rarity : "dustlight";
            String rarityColor = plugin.getConfigManager().getRarityConfig()
                    .getString(r + ".color", "<white>");
            String rarityName = itemManager.getRarityDisplayName(player, r);
            return plugin.getLanguageManager().getItemName(player, "stellaris-codex")
                    .replace("{rarity_color}", rarityColor)
                    .replace("{rarity_name}", rarityName);
        }
        if (itemManager.isDisenchantItemType(itemType)) {
            return itemManager.getDisenchantItemDisplayName(player, itemType);
        }
        if ("anvil-breakthrough-stone".equals(itemType)) {
            return plugin.getLanguageManager().getItemName(player, "anvil-breakthrough-stone");
        }
        return itemType;
    }

    private List<String> itemTypes() {
        List<String> types = new ArrayList<>();
        types.add("fragment");
        types.add("codex");
        types.addAll(plugin.getCustomItemManager().getDisenchantItemTypes());
        types.add("anvil-breakthrough-stone");
        return types;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
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
            String input = args[1].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (String type : itemTypes()) {
                if (type.startsWith(input)) {
                    completions.add(type);
                }
            }
            return completions;
        }

        if (args.length == 3) {
            return Arrays.asList("1", "5", "10", "32", "64");
        }

        if (args.length == 4 && "codex".equals(args[1].toLowerCase())) {
            String input = args[3].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (String r : RARITIES) {
                if (r.startsWith(input)) {
                    completions.add(r);
                }
            }
            return completions;
        }

        return Collections.emptyList();
    }
}
