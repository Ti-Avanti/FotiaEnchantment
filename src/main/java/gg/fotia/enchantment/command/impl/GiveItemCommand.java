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

/**
 * /fe giveitem <player> <item_type> [amount] [rarity]
 * 给玩家自定义道具
 */
public class GiveItemCommand implements SubCommand {

    private static final List<String> ITEM_TYPES = Arrays.asList(
            "fragment", "codex", "disenchant-shard", "disenchant-crystal", "disenchant-gem",
            "anvil-breakthrough-stone"
    );

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

        // 获取道具类型
        String itemType = args[1].toLowerCase();
        if (!ITEM_TYPES.contains(itemType)) {
            if (sender instanceof Player player) {
                messageHelper.sendMessage(player, "item-not-found", Map.of("item_id", itemType));
            } else {
                sender.sendMessage("Item type " + itemType + " not found.");
            }
            return;
        }

        // 获取数量
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1) amount = 1;
                if (amount > 64) amount = 64;
            } catch (NumberFormatException e) {
                amount = 1;
            }
        }

        // 获取稀有度（仅 codex 需要）
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

    /**
     * 创建指定自定义道具。
     */
    private ItemStack createItem(Player player, String itemType, String rarity, int amount) {
        CustomItemManager itemManager = plugin.getCustomItemManager();
        ItemStack item = switch (itemType) {
            case "fragment" -> itemManager.createStarweaveFragment(player, amount);
            case "codex" -> itemManager.createStellarisCodex(
                    player, rarity != null ? rarity : "dustlight");
            case "disenchant-shard" -> itemManager.createDisenchantStone(player, "tier-1");
            case "disenchant-crystal" -> itemManager.createDisenchantStone(player, "tier-2");
            case "disenchant-gem" -> itemManager.createDisenchantStone(player, "tier-3");
            case "anvil-breakthrough-stone" -> itemManager.createAnvilBreakthroughStone(player, amount);
            default -> null;
        };
        if (item != null) {
            item.setAmount(amount);
        }
        return item;
    }

    /**
     * 给予玩家物品，背包放不下时掉落在玩家脚下。
     */
    private void giveItem(Player player, ItemStack item) {
        var leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    /**
     * 获取道具显示名称
     */
    private String getItemDisplayName(Player player, String itemType, String rarity) {
        return switch (itemType) {
            case "fragment" -> plugin.getLanguageManager().getItemName(player, "starweave-fragment");
            case "codex" -> {
                String r = rarity != null ? rarity : "dustlight";
                String rarityColor = plugin.getConfigManager().getRarityConfig()
                        .getString(r + ".color", "<white>");
                String rarityName = plugin.getCustomItemManager().getRarityDisplayName(player, r);
                yield plugin.getLanguageManager().getItemName(player, "stellaris-codex")
                        .replace("{rarity_color}", rarityColor)
                        .replace("{rarity_name}", rarityName);
            }
            case "disenchant-shard" -> plugin.getLanguageManager().getItemName(player, "disenchant-shard");
            case "disenchant-crystal" -> plugin.getLanguageManager().getItemName(player, "disenchant-crystal");
            case "disenchant-gem" -> plugin.getLanguageManager().getItemName(player, "disenchant-gem");
            case "anvil-breakthrough-stone" -> plugin.getLanguageManager().getItemName(player, "anvil-breakthrough-stone");
            default -> itemType;
        };
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
            // 补全道具类型
            String input = args[1].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (String type : ITEM_TYPES) {
                if (type.startsWith(input)) {
                    completions.add(type);
                }
            }
            return completions;
        }

        if (args.length == 3) {
            // 补全数量
            return Arrays.asList("1", "5", "10", "32", "64");
        }

        if (args.length == 4) {
            // 补全稀有度（仅 codex 类型需要）
            if ("codex".equals(args[1].toLowerCase())) {
                String input = args[3].toLowerCase();
                List<String> completions = new ArrayList<>();
                for (String r : RARITIES) {
                    if (r.startsWith(input)) {
                        completions.add(r);
                    }
                }
                return completions;
            }
        }

        return Collections.emptyList();
    }
}
