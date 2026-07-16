package gg.fotia.enchantment.mining;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.util.SchedulerUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记录玩家放置的矿石，防止精准采集后重复放置、挖掘来反复触发额外掉落。
 * 标记保存在区块 PDC 中，因此区块卸载或服务器重启后仍然有效。
 */
public final class NaturalOreTracker implements Listener {

    private static final String CONFIG_PATH = "mining.natural-ore-protection";

    private final FotiaEnchantment plugin;
    private final NamespacedKey placedBlocksKey;
    private final Map<ChunkKey, Set<Integer>> placedBlocks = new ConcurrentHashMap<>();
    private final Set<BlockKey> recentlyBrokenPlacedBlocks = ConcurrentHashMap.newKeySet();

    private volatile boolean enabled;
    private volatile List<MaterialPattern> trackedMaterials = List.of();

    public NaturalOreTracker(FotiaEnchantment plugin) {
        this.plugin = plugin;
        this.placedBlocksKey = new NamespacedKey(plugin, "player_placed_mining_blocks");
    }

    public void init() {
        reload();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void reload() {
        YamlConfiguration config = plugin.getConfigManager().getMainConfig();
        enabled = config.getBoolean(CONFIG_PATH + ".enabled", true);

        List<MaterialPattern> patterns = new ArrayList<>();
        for (String value : config.getStringList(CONFIG_PATH + ".tracked-materials")) {
            MaterialPattern pattern = MaterialPattern.parse(value);
            if (pattern != null) {
                patterns.add(pattern);
            }
        }
        if (patterns.isEmpty()) {
            patterns.add(MaterialPattern.parse("*_ORE"));
            patterns.add(MaterialPattern.parse("ANCIENT_DEBRIS"));
        }
        trackedMaterials = List.copyOf(patterns);
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
        placedBlocks.clear();
        recentlyBrokenPlacedBlocks.clear();
    }

    public boolean isPlayerPlacedOre(Block block) {
        return block != null && isPlayerPlacedOre(block.getLocation(), block.getType());
    }

    public boolean isPlayerPlacedOre(BlockState state) {
        return state != null && isPlayerPlacedOre(state.getLocation(), state.getType());
    }

    public boolean isPlayerPlacedOre(Location location, Material material) {
        if (!enabled || location == null || material == null || !isTrackedMaterial(material)) {
            return false;
        }
        BlockKey blockKey = BlockKey.from(location);
        if (recentlyBrokenPlacedBlocks.contains(blockKey)) {
            return true;
        }
        Chunk chunk = location.getChunk();
        return positions(chunk).contains(pack(location));
    }

    public boolean isTrackedMaterial(Material material) {
        if (material == null || !material.isBlock()) {
            return false;
        }
        String name = material.name();
        for (MaterialPattern pattern : trackedMaterials) {
            if (pattern.matches(name)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (enabled && isTrackedMaterial(block.getType())) {
            mark(block.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        if (!unmark(location)) {
            return;
        }

        BlockKey blockKey = BlockKey.from(location);
        recentlyBrokenPlacedBlocks.add(blockKey);
        SchedulerUtils.runTaskLater(plugin, () -> recentlyBrokenPlacedBlocks.remove(blockKey), 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        clearDestroyedBlocks(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        clearDestroyedBlocks(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        transferMovedBlocks(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        transferMovedBlocks(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        placedBlocks.remove(ChunkKey.from(event.getChunk()));
    }

    private void clearDestroyedBlocks(List<Block> blocks) {
        for (Block block : blocks) {
            unmark(block.getLocation());
        }
    }

    private void transferMovedBlocks(List<Block> blocks, BlockFace direction) {
        if (!enabled || blocks == null || blocks.isEmpty()) {
            return;
        }

        List<Location> markedSources = new ArrayList<>();
        for (Block block : blocks) {
            if (isPlayerPlacedOre(block)) {
                markedSources.add(block.getLocation());
            }
        }
        if (markedSources.isEmpty()) {
            return;
        }

        for (Location source : markedSources) {
            unmark(source);
        }
        for (Location source : markedSources) {
            mark(source.clone().add(direction.getModX(), direction.getModY(), direction.getModZ()));
        }
    }

    private void mark(Location location) {
        Chunk chunk = location.getChunk();
        Set<Integer> positions = positions(chunk);
        if (positions.add(pack(location))) {
            persist(chunk, positions);
        }
    }

    private boolean unmark(Location location) {
        Chunk chunk = location.getChunk();
        Set<Integer> positions = positions(chunk);
        boolean removed = positions.remove(pack(location));
        if (removed) {
            persist(chunk, positions);
        }
        return removed;
    }

    private Set<Integer> positions(Chunk chunk) {
        ChunkKey key = ChunkKey.from(chunk);
        return placedBlocks.computeIfAbsent(key, ignored -> load(chunk));
    }

    private Set<Integer> load(Chunk chunk) {
        Set<Integer> result = ConcurrentHashMap.newKeySet();
        int[] stored = chunk.getPersistentDataContainer().get(
                placedBlocksKey, PersistentDataType.INTEGER_ARRAY);
        if (stored != null) {
            Arrays.stream(stored).forEach(result::add);
        }
        return result;
    }

    private void persist(Chunk chunk, Set<Integer> positions) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (positions.isEmpty()) {
            pdc.remove(placedBlocksKey);
            return;
        }
        int[] packedPositions = positions.stream().mapToInt(Integer::intValue).sorted().toArray();
        pdc.set(placedBlocksKey, PersistentDataType.INTEGER_ARRAY, packedPositions);
    }

    private int pack(Location location) {
        int localX = location.getBlockX() & 15;
        int localZ = location.getBlockZ() & 15;
        int y = location.getBlockY() & 0xFFF;
        return (y << 8) | (localZ << 4) | localX;
    }

    private record ChunkKey(UUID worldId, int x, int z) {
        static ChunkKey from(Chunk chunk) {
            return new ChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        }
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {
        static BlockKey from(Location location) {
            return new BlockKey(location.getWorld().getUID(),
                    location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
    }

    private record MaterialPattern(String value, MatchMode mode) {
        static MaterialPattern parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String normalized = raw.trim().toUpperCase(Locale.ROOT)
                    .replace("MINECRAFT:", "")
                    .replace('-', '_');
            if (normalized.equals("*")) {
                return new MaterialPattern("", MatchMode.ALL);
            }
            if (normalized.startsWith("*") && normalized.length() > 1) {
                return new MaterialPattern(normalized.substring(1), MatchMode.SUFFIX);
            }
            if (normalized.endsWith("*") && normalized.length() > 1) {
                return new MaterialPattern(normalized.substring(0, normalized.length() - 1), MatchMode.PREFIX);
            }
            return new MaterialPattern(normalized, MatchMode.EXACT);
        }

        boolean matches(String materialName) {
            return switch (mode) {
                case ALL -> true;
                case EXACT -> materialName.equals(value);
                case PREFIX -> materialName.startsWith(value);
                case SUFFIX -> materialName.endsWith(value);
            };
        }
    }

    private enum MatchMode {
        ALL,
        EXACT,
        PREFIX,
        SUFFIX
    }
}
