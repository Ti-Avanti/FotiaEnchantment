package gg.fotia.enchantment.integration;

import gg.fotia.enchantment.FotiaEnchantment;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class RequiredPluginChecker {

    private static final List<String> REQUIRED_PLUGINS = List.of("packetevents");

    private RequiredPluginChecker() {
    }

    public static boolean verifyOrDisable(FotiaEnchantment plugin) {
        List<String> missing = missingRequiredPlugins(plugin.getServer().getPluginManager());
        if (missing.isEmpty()) {
            return true;
        }

        plugin.getLogger().severe("缺少必需前置插件，FotiaEnchantment 将不会启用。");
        plugin.getLogger().severe("缺少前置: " + String.join(", ", missing));
        plugin.getLogger().severe("请安装 PacketEvents 后重启服务器: https://modrinth.com/plugin/packetevents");
        plugin.getServer().getPluginManager().disablePlugin(plugin);
        return false;
    }

    static List<String> missingRequiredPlugins(PluginManager pluginManager) {
        List<String> installed = new ArrayList<>();
        for (Plugin plugin : pluginManager.getPlugins()) {
            if (plugin != null) {
                installed.add(plugin.getName());
            }
        }
        return missingRequiredPlugins(installed);
    }

    static List<String> missingRequiredPlugins(Collection<String> installedPluginNames) {
        Set<String> installed = installedPluginNames == null
                ? Set.of()
                : installedPluginNames.stream()
                .map(name -> name == null ? "" : name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<String> missing = new ArrayList<>();
        for (String required : REQUIRED_PLUGINS) {
            if (!installed.contains(required.toLowerCase(Locale.ROOT))) {
                missing.add(required);
            }
        }
        return List.copyOf(missing);
    }
}
