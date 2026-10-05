package gg.fotia.enchantment.integration;

import gg.fotia.enchantment.FotiaEnchantment;
import gg.fotia.enchantment.compat.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RequiredPluginChecker {

    private static final List<String> REQUIRED_PLUGINS = List.of("packetevents");
    private static final Pattern PLUGIN_VERSION = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?.*");

    private RequiredPluginChecker() {
    }

    public static boolean verifyPacketEventsVersion(FotiaEnchantment plugin) {
        Plugin dependency = plugin.getServer().getPluginManager().getPlugin("packetevents");
        String installed = dependency == null ? "unknown" : dependency.getPluginMeta().getVersion();
        String minecraft = Bukkit.getMinecraftVersion();
        if (supportsPacketEvents(minecraft, installed)) {
            return true;
        }
        String message = plugin.getLanguageManager().getMessage(
                plugin.getLanguageManager().getDefaultLanguage(), "dependency-incompatible")
                .replace("{minecraft}", minecraft)
                .replace("{installed}", installed)
                .replace("{required}", "2." + minimumPacketEventsMinor(minecraft) + ".0");
        plugin.getServer().getConsoleSender().sendMessage(plugin.getMessageHelper().parseText(message));
        plugin.getServer().getPluginManager().disablePlugin(plugin);
        return false;
    }

    static boolean supportsPacketEvents(String minecraft, String installed) {
        int minimum = minimumPacketEventsMinor(minecraft);
        // 旧服务器保留原有前置要求，版本门槛只针对新增的协议。
        if (minimum == 0) return true;
        Matcher matcher = PLUGIN_VERSION.matcher(installed == null ? "" : installed.trim());
        if (!matcher.matches()) return false;
        try {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            return major > 2 || major == 2 && minor >= minimum;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static int minimumPacketEventsMinor(String minecraft) {
        MinecraftVersion version = MinecraftVersion.parse(minecraft);
        if (version.compareTo(new MinecraftVersion(26, 3, 0)) >= 0) return 14;
        if (version.compareTo(new MinecraftVersion(26, 2, 0)) >= 0) return 13;
        return 0;
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
