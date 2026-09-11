package gg.fotia.enchantment.reload;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.config.ConfigManager;
import gg.fotia.enchantment.config.RuntimeConfiguration;
import gg.fotia.enchantment.config.VanillaConfig;
import gg.fotia.enchantment.core.EnchantmentManager;
import gg.fotia.enchantment.lang.LanguageManager;
import gg.fotia.enchantment.lang.MessageHelper;
import gg.fotia.enchantment.util.SchedulerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/** 串行准备配置变更，文件操作留在后台，发布和通知回到所属线程。 */
public final class PluginReloadService {
    private final FotiaEnchantment plugin;
    private final AtomicBoolean busy = new AtomicBoolean();
    private volatile boolean closed;

    public PluginReloadService(FotiaEnchantment plugin) {
        this.plugin = plugin;
    }

    public boolean isBusy() {
        return busy.get();
    }

    public void shutdown() {
        closed = true;
    }

    public void reload(CommandSender sender, Runnable completed) {
        if (!begin(sender)) {
            return;
        }
        try {
            Map<String, String> defaults = plugin.getVanillaManager().getVanillaConfig().captureDefaultFiles();
            ConfigManager currentConfig = plugin.getConfigManager();
            LanguageManager currentLanguage = plugin.getLanguageManager();
            notifySender(sender, "reload-started");
            SchedulerUtils.runAsyncTask(plugin, () -> {
                try {
                    ConfigManager config = currentConfig.prepareReload();
                    LanguageManager language = currentLanguage.prepareReload(config);
                    EnchantmentManager enchantments = new EnchantmentManager(plugin);
                    enchantments.init();
                    VanillaConfig vanilla = new VanillaConfig(plugin);
                    vanilla.loadAll(defaults);
                    RuntimeConfiguration prepared = new RuntimeConfiguration(config, language,
                            enchantments, vanilla, new MessageHelper(plugin, language));
                    publish(sender, () -> {
                        plugin.getEffectPipeline().pause();
                        plugin.publishConfiguration(prepared);
                        plugin.getNaturalOreTracker().reload();
                        plugin.getEnchantmentDisplayListener().reload();
                        plugin.getEffectPipeline().reload();
                    }, completed);
                } catch (Exception ex) {
                    fail(sender, ex);
                }
            });
        } catch (Exception ex) {
            fail(sender, ex);
        }
    }

    public void toggle(Player player, String enchantmentId, boolean enabled, Runnable completed) {
        if (!begin(player)) {
            return;
        }
        EnchantmentManager manager = plugin.getEnchantmentManager();
        try {
            SchedulerUtils.runAsyncTask(plugin, () -> {
                try {
                    if (!manager.getEnchantmentConfig().persistEnabled(enchantmentId, enabled)) {
                        throw new IllegalStateException("Unable to save enchantment: " + enchantmentId);
                    }
                    publish(player, () -> manager.applyEnabled(enchantmentId, enabled), completed);
                } catch (Exception ex) {
                    fail(player, ex);
                }
            });
        } catch (Exception ex) {
            fail(player, ex);
        }
    }

    private boolean begin(CommandSender sender) {
        if (closed) {
            return false;
        }
        if (busy.compareAndSet(false, true)) {
            return true;
        }
        notifySender(sender, "reload-in-progress");
        return false;
    }

    private void publish(CommandSender sender, Runnable publish, Runnable completed) {
        if (closed || !plugin.isEnabled()) {
            busy.set(false);
            return;
        }
        SchedulerUtils.runTask(plugin, () -> {
            try {
                if (closed) {
                    return;
                }
                publish.run();
                onSenderThread(sender, completed);
            } catch (Exception ex) {
                reportFailure(sender, ex);
            } finally {
                busy.set(false);
            }
        });
    }

    private void fail(CommandSender sender, Exception ex) {
        busy.set(false);
        reportFailure(sender, ex);
    }

    private void reportFailure(CommandSender sender, Exception ex) {
        plugin.getLogger().log(Level.WARNING, "Configuration update failed", ex);
        try {
            notifySender(sender, "reload-failed");
        } catch (org.bukkit.plugin.IllegalPluginAccessException ignored) {
            // 停服过程中不再注册通知任务。
        }
    }

    private void notifySender(CommandSender sender, String key) {
        onSenderThread(sender, () -> {
            if (sender instanceof Player player) {
                plugin.getMessageHelper().sendMessage(player, key);
            } else {
                String message = plugin.getLanguageManager().getMessage(
                        plugin.getLanguageManager().getDefaultLanguage(), key);
                sender.sendMessage(plugin.getMessageHelper().parseText(null, message, Map.of()));
            }
        });
    }

    private void onSenderThread(CommandSender sender, Runnable action) {
        if (closed || !plugin.isEnabled()) {
            return;
        }
        if (sender instanceof Player player) {
            SchedulerUtils.runEntityTask(plugin, player, () -> {
                if (!closed && player.isOnline()) {
                    action.run();
                }
            });
        } else {
            SchedulerUtils.runTask(plugin, action);
        }
    }
}
