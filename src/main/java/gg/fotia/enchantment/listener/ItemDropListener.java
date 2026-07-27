package gg.fotia.enchantment.listener;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.item.CustomItemManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 道具掉落监听器
 * <p>
 * 1. 怪物死亡时按 config.yml 中 item-drops.mob-drop 配置概率掉落
 * 2. 玩家挖矿时按 config.yml 中 item-drops.mining-drop 配置概率掉落
 * 仅当击杀者/挖掘者为玩家时触发
 * <p>
 * 掉落表按配置代数惰性解析为 EnumMap, 事件热路径不再重复读取 YAML。
 */
public class ItemDropListener implements Listener {

    private final FotiaEnchantment plugin;
    private final CustomItemManager itemManager;

    private volatile DropTables tables;
    private volatile int tablesGeneration = Integer.MIN_VALUE;

    public ItemDropListener(FotiaEnchantment plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getCustomItemManager();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        DropTables dropTables = tables();
        if (!dropTables.mobEnabled || dropTables.mobs.isEmpty()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }

        DropEntry entry = dropTables.mobs.get(entity.getType());
        if (entry == null) {
            return;
        }

        if (rollChance(entry.chance())) {
            String itemId = pickRandom(entry.items());
            ItemStack drop = createItem(killer, itemId, 1);
            if (drop != null) {
                event.getDrops().add(drop);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        DropTables dropTables = tables();
        if (!dropTables.miningEnabled || dropTables.blocks.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        DropEntry entry = dropTables.blocks.get(event.getBlock().getType());
        if (entry == null) {
            return;
        }
        // 玩家放置矿石检查放在掉落表命中之后, 未配置掉落的方块不触达追踪器
        if (plugin.getNaturalOreTracker().isPlayerPlacedOre(event.getBlock())) {
            return;
        }

        // 挖矿掉落物直接落地
        if (rollChance(entry.chance())) {
            String itemId = pickRandom(entry.items());
            ItemStack drop = createItem(player, itemId, 1);
            if (drop != null) {
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
            }
        }
    }

    /**
     * 获取当前掉落表, 配置代数变化时重建
     */
    private DropTables tables() {
        int generation = plugin.getConfigManager().getConfigGeneration();
        DropTables current = tables;
        if (current != null && tablesGeneration == generation) {
            return current;
        }
        DropTables rebuilt = buildTables(plugin.getConfigManager().getMainConfig());
        tables = rebuilt;
        tablesGeneration = generation;
        return rebuilt;
    }

    private DropTables buildTables(YamlConfiguration config) {
        boolean mobEnabled = config.getBoolean("item-drops.mob-drop.enabled", false);
        boolean miningEnabled = config.getBoolean("item-drops.mining-drop.enabled", false);
        double defaultChance = config.getDouble("item-drops.mob-drop.default-chance", 0.0);

        Map<EntityType, DropEntry> mobs = new EnumMap<>(EntityType.class);
        ConfigurationSection mobsSection = config.getConfigurationSection("item-drops.mob-drop.mobs");
        if (mobsSection != null) {
            for (String key : mobsSection.getKeys(false)) {
                ConfigurationSection mobConfig = mobsSection.getConfigurationSection(key);
                if (mobConfig == null) {
                    continue;
                }
                EntityType type;
                try {
                    type = EntityType.valueOf(key.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("item-drops.mob-drop.mobs 中存在未知实体类型: " + key);
                    continue;
                }
                List<String> items = List.copyOf(mobConfig.getStringList("items"));
                if (items.isEmpty()) {
                    continue;
                }
                mobs.put(type, new DropEntry(mobConfig.getDouble("chance", defaultChance), items));
            }
        }

        Map<Material, DropEntry> blocks = new EnumMap<>(Material.class);
        ConfigurationSection blocksSection = config.getConfigurationSection("item-drops.mining-drop.blocks");
        if (blocksSection != null) {
            for (String key : blocksSection.getKeys(false)) {
                ConfigurationSection blockConfig = blocksSection.getConfigurationSection(key);
                if (blockConfig == null) {
                    continue;
                }
                Material material = Material.matchMaterial(key.trim());
                if (material == null) {
                    plugin.getLogger().warning("item-drops.mining-drop.blocks 中存在未知方块类型: " + key);
                    continue;
                }
                List<String> items = List.copyOf(blockConfig.getStringList("items"));
                if (items.isEmpty()) {
                    continue;
                }
                blocks.put(material, new DropEntry(blockConfig.getDouble("chance", 0.0), items));
            }
        }

        return new DropTables(mobEnabled, miningEnabled, mobs, blocks);
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

    private record DropTables(boolean mobEnabled,
                              boolean miningEnabled,
                              Map<EntityType, DropEntry> mobs,
                              Map<Material, DropEntry> blocks) {
    }

    private record DropEntry(double chance, List<String> items) {
    }
}
