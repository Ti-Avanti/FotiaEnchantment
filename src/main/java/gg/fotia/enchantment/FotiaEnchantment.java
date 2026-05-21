package gg.fotia.enchantment;

import gg.fotia.enchantment.command.CommandManager;
import gg.fotia.enchantment.config.ConfigManager;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.pipeline.EffectPipeline;
import gg.fotia.enchantment.core.VanillaManager;
import gg.fotia.enchantment.gui.GUIManager;
import gg.fotia.enchantment.integration.IntegrationManager;
import gg.fotia.enchantment.item.CustomItemManager;
import gg.fotia.enchantment.lang.LanguageManager;
import gg.fotia.enchantment.lang.MessageHelper;
import gg.fotia.enchantment.listener.EnchantListener;
import gg.fotia.enchantment.listener.EnchantmentDisplayListener;
import gg.fotia.enchantment.listener.ItemDropListener;
import gg.fotia.enchantment.listener.ItemUseListener;
import gg.fotia.enchantment.listener.TradeListener;
import gg.fotia.enchantment.update.UpdateChecker;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class FotiaEnchantment extends JavaPlugin {

    private static FotiaEnchantment instance;
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private MessageHelper messageHelper;
    private EnchantmentManager enchantmentManager;
    private EffectPipeline effectPipeline;
    private VanillaManager vanillaManager;
    private CustomItemManager customItemManager;
    private IntegrationManager integrationManager;
    private GUIManager guiManager;
    private CommandManager commandManager;
    private UpdateChecker updateChecker;

    @Override
    public void onLoad() {
        instance = this;
        // WorldGuard Flag 必须在 onLoad 注册
        integrationManager = new IntegrationManager(this);
        integrationManager.onLoad();
    }

    @Override
    public void onEnable() {
        instance = this;

        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.loadAll();

        // 初始化多语言系统
        languageManager = new LanguageManager(this);
        languageManager.init();
        messageHelper = new MessageHelper(this, languageManager);

        // 初始化附魔管理器
        enchantmentManager = new EnchantmentManager(this);
        enchantmentManager.init();

        // 初始化原版附魔覆盖管理器
        vanillaManager = new VanillaManager(this);
        vanillaManager.init();

        // 初始化自定义道具管理器
        customItemManager = new CustomItemManager(this);
        customItemManager.init();

        // 初始化集成管理器
        if (integrationManager == null) {
            integrationManager = new IntegrationManager(this);
        }
        integrationManager.onEnable();

        // 初始化 GUI 管理器
        guiManager = new GUIManager(this);

        // 初始化效果管道
        effectPipeline = new EffectPipeline(this);
        effectPipeline.init();

        // 初始化命令管理器
        commandManager = new CommandManager(this);
        commandManager.init();

        updateChecker = new UpdateChecker(this);
        updateChecker.checkOnStartup();

        // 注册监听器
        registerListeners();

        getLogger().info("FotiaEnchantment 已启用");
    }

    @Override
    public void onDisable() {
        if (effectPipeline != null) {
            effectPipeline.shutdown();
        }
        if (integrationManager != null) {
            integrationManager.shutdown();
        }
        if (vanillaManager != null) {
            vanillaManager.shutdown();
        }
        if (enchantmentManager != null) {
            enchantmentManager.shutdown();
        }
        if (updateChecker != null) {
            updateChecker.shutdown();
        }
        getLogger().info("FotiaEnchantment 已禁用");
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(guiManager, this);
        pm.registerEvents(new ItemUseListener(this), this);
        pm.registerEvents(new ItemDropListener(this), this);
        pm.registerEvents(new EnchantListener(this), this);
        pm.registerEvents(new EnchantmentDisplayListener(this), this);
        pm.registerEvents(new TradeListener(this), this);
    }

    public static FotiaEnchantment getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public MessageHelper getMessageHelper() {
        return messageHelper;
    }

    public EnchantmentManager getEnchantmentManager() {
        return enchantmentManager;
    }

    public VanillaManager getVanillaManager() {
        return vanillaManager;
    }

    public CustomItemManager getCustomItemManager() {
        return customItemManager;
    }

    public IntegrationManager getIntegrationManager() {
        return integrationManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public EffectPipeline getEffectPipeline() {
        return effectPipeline;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}
