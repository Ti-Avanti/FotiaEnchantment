package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.item.CustomItemManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 道具掉落监听器
 * <p>
 * 1. 怪物死亡时按 config.yml 中 item-drops.mob-drop 配置概率掉落
 * 2. 玩家挖矿时按 config.yml 中 item-drops.mining-drop 配置概率掉落
 * 仅当击杀者/挖掘者为玩家时触发
 */
public class ItemDropListener implements Listener {

    private final FotiaEnchantment plugin;
    private final CustomItemManager itemManager;

    public ItemDropListener(FotiaEnchantment plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getCustomItemManager();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        YamlConfiguration mainConfig = plugin.getConfigManager().getMainConfig();
        if (!mainConfig.getBoolean("item-drops.mob-drop.enabled", false)) {
            return;
        }

        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }

        ConfigurationSection mobsSection = mainConfig.getConfigurationSection("item-drops.mob-drop.mobs");
        if (mobsSection == null) {
            return;
        }

        String mobKey = entity.getType().name().toUpperCase(Locale.ROOT);
        ConfigurationSection mobConfig = mobsSection.getConfigurationSection(mobKey);
        if (mobConfig == null) {
            return;
        }

        double defaultChance = mainConfig.getDouble("item-drops.mob-drop.default-chance", 0.0);
        double chance = mobConfig.getDouble("chance", defaultChance);
        List<String> items = mobConfig.getStringList("items");
        if (items.isEmpty()) {
            return;
        }

        rollAndDrop(event, killer, chance, items);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        YamlConfiguration mainConfig = plugin.getConfigManager().getMainConfig();
        if (!mainConfig.getBoolean("item-drops.mining-drop.enabled", false)) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (plugin.getNaturalOreTracker().isPlayerPlacedOre(event.getBlock())) {
            return;
        }

        ConfigurationSection blocksSection = mainConfig.getConfigurationSection("item-drops.mining-drop.blocks");
        if (blocksSection == null) {
            return;
        }

        Material type = event.getBlock().getType();
        String blockKey = type.name().toUpperCase(Locale.ROOT);
        ConfigurationSection blockConfig = blocksSection.getConfigurationSection(blockKey);
        if (blockConfig == null) {
            return;
        }

        double chance = blockConfig.getDouble("chance", 0.0);
        List<String> items = blockConfig.getStringList("items");
        if (items.isEmpty()) {
            return;
        }

        // 挖矿掉落物直接落地
        if (rollChance(chance)) {
            String itemId = pickRandom(items);
            ItemStack drop = createItem(player, itemId, 1);
            if (drop != null) {
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
            }
        }
    }

    /**
     * 怪物掉落: 按概率添加到 EntityDeathEvent 的掉落列表
     */
    private void rollAndDrop(EntityDeathEvent event, Player player, double chance, List<String> items) {
        if (!rollChance(chance)) {
            return;
        }
        String itemId = pickRandom(items);
        ItemStack drop = createItem(player, itemId, 1);
        if (drop != null) {
            event.getDrops().add(drop);
        }
    }

    /**
     * 按百分比概率(0-100, 支持小数)进行一次随机判定
     */
    private boolean rollChance(double chancePercent) {
        if (chancePercent <= 0) return false;
        if (chancePercent >= 100) return true;
        return ThreadLocalRandom.current().nextDouble(100.0) < chancePercent;
    }

    private String pickRandom(List<String> list) {
        if (list.size() == 1) return list.get(0);
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    /**
     * 根据道具ID创建对应物品
     */
    private ItemStack createItem(Player player, String itemId, int amount) {
        if (itemId == null) return null;
        return switch (itemId.toLowerCase(Locale.ROOT)) {
            case "starweave-fragment" -> itemManager.createStarweaveFragment(player, amount);
            case "disenchant-shard" -> itemManager.createDisenchantStone(player, "tier-1");
            case "disenchant-crystal" -> itemManager.createDisenchantStone(player, "tier-2");
            case "disenchant-gem" -> itemManager.createDisenchantStone(player, "tier-3");
            default -> null;
        };
    }
}
