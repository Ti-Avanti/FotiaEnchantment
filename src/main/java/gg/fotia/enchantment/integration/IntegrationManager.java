package gg.fotia.enchantment.integration;

import gg.fotia.enchantment.FotiaEnchantment;
import org.bukkit.Bukkit;

import java.util.logging.Logger;

/**
 * 集成管理器
 * <p>
 * 统一负责所有第三方插件 Hook 的检测、初始化与生命周期管理。
 * 各 Hook 在内部自行判断对应插件是否存在; 本管理器只暴露统一的 getter。
 */
public class IntegrationManager {

    private final FotiaEnchantment plugin;
    private final Logger logger;

    private WorldGuardHook worldGuardHook;
    private CraftEngineHook craftEngineHook;
    private AuraSkillsHook auraSkillsHook;
    private McMMOHook mcMMOHook;
    private MythicMobsHook mythicMobsHook;
    private PacketEventsHook packetEventsHook;
    private PlaceholderAPIHook placeholderAPIHook;

    public IntegrationManager(FotiaEnchantment plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * 在 onLoad 阶段调用 (主要给 WorldGuard 注册 Flag 用)
     */
    public void onLoad() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardHook = new WorldGuardHook(logger);
            worldGuardHook.registerFlags();
        }
    }

    /**
     * 在 onEnable 阶段调用, 初始化所有可用集成
     */
    public void onEnable() {
        // WorldGuard 可能已经在 onLoad 中初始化
        if (worldGuardHook == null && Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardHook = new WorldGuardHook(logger);
        }

        if (Bukkit.getPluginManager().getPlugin("CraftEngine") != null) {
            try {
                craftEngineHook = new CraftEngineHook(logger);
            } catch (LinkageError | RuntimeException e) {
                logger.warning("检测到 CraftEngine，但 API 类不可访问，已禁用 CraftEngine 集成: "
                        + e.getMessage());
            }
        }
        if (Bukkit.getPluginManager().getPlugin("AuraSkills") != null) {
            auraSkillsHook = new AuraSkillsHook(logger);
        }
        if (Bukkit.getPluginManager().getPlugin("mcMMO") != null) {
            mcMMOHook = new McMMOHook(logger);
        }
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") != null) {
            mythicMobsHook = new MythicMobsHook(logger);
        }
        if (Bukkit.getPluginManager().getPlugin("packetevents") != null) {
            initPacketEventsHook();
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIHook = new PlaceholderAPIHook(plugin);
            placeholderAPIHook.register();
        }

        logger.info("集成管理器已初始化, 可用 Hook: " + summary());
    }

    /**
     * 关闭所有集成
     */
    public void shutdown() {
        if (packetEventsHook != null) {
            packetEventsHook.shutdown();
        }
        if (placeholderAPIHook != null) {
            placeholderAPIHook.unregister();
        }
    }

    private String summary() {
        StringBuilder sb = new StringBuilder();
        appendHook(sb, "WorldGuard", worldGuardHook != null && worldGuardHook.isAvailable());
        appendHook(sb, "CraftEngine", craftEngineHook != null && craftEngineHook.isAvailable());
        appendHook(sb, "AuraSkills", auraSkillsHook != null && auraSkillsHook.isAvailable());
        appendHook(sb, "mcMMO", mcMMOHook != null && mcMMOHook.isAvailable());
        appendHook(sb, "MythicMobs", mythicMobsHook != null && mythicMobsHook.isAvailable());
        appendHook(sb, "packetevents", packetEventsHook != null && packetEventsHook.isAvailable());
        appendHook(sb, "PlaceholderAPI", placeholderAPIHook != null && placeholderAPIHook.isAvailable());
        return sb.length() == 0 ? "无" : sb.toString();
    }

    private void appendHook(StringBuilder sb, String name, boolean available) {
        if (available) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(name);
        }
    }

    private void initPacketEventsHook() {
        if (packetEventsHook != null && packetEventsHook.isAvailable()) {
            return;
        }
        if (Bukkit.getPluginManager().isPluginEnabled("packetevents")) {
            packetEventsHook = new PacketEventsHook(plugin);
            packetEventsHook.init();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (packetEventsHook != null && packetEventsHook.isAvailable()) {
                return;
            }
            if (Bukkit.getPluginManager().isPluginEnabled("packetevents")) {
                packetEventsHook = new PacketEventsHook(plugin);
                packetEventsHook.init();
            }
        });
    }

    // ==================== Getter ====================

    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }

    public CraftEngineHook getCraftEngineHook() {
        return craftEngineHook;
    }

    public AuraSkillsHook getAuraSkillsHook() {
        return auraSkillsHook;
    }

    public McMMOHook getMcMMOHook() {
        return mcMMOHook;
    }

    public MythicMobsHook getMythicMobsHook() {
        return mythicMobsHook;
    }

    public PacketEventsHook getPacketEventsHook() {
        return packetEventsHook;
    }

    public PlaceholderAPIHook getPlaceholderAPIHook() {
        return placeholderAPIHook;
    }
}
