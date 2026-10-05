package gg.fotia.enchantment.bootstrap;

import gg.fotia.enchantment.bootstrap.api.FotiaBootstrapImplementation;
import gg.fotia.enchantment.bootstrap.paper.v1_21_R1.PaperV1_21_R1Bootstrap;
import gg.fotia.enchantment.bootstrap.paper.v1_21_R6.PaperV1_21_R6Bootstrap;
import gg.fotia.enchantment.compat.MinecraftVersion;
import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import org.bukkit.Bukkit;

public final class FotiaEnchantmentBootstrap implements PluginBootstrap {

    private static final FotiaBootstrapImplementation NO_REGISTRY_BOOTSTRAP = context -> {
    };
    private static final MinecraftVersion REGISTRY_BOOTSTRAP_MIN_VERSION = new MinecraftVersion(1, 21, 4);
    private static final MinecraftVersion COMPOSE_REGISTRY_MIN_VERSION = new MinecraftVersion(1, 21, 11);

    @Override
    public void bootstrap(BootstrapContext context) {
        implementationFor(currentMinecraftVersion()).bootstrap(context);
    }

    static FotiaBootstrapImplementation implementationFor(String minecraftVersionId) {
        MinecraftVersion version = MinecraftVersion.parse(minecraftVersionId);
        if (version.compareTo(COMPOSE_REGISTRY_MIN_VERSION) >= 0) {
            return new PaperV1_21_R6Bootstrap();
        }
        if (version.compareTo(REGISTRY_BOOTSTRAP_MIN_VERSION) >= 0) {
            return new PaperV1_21_R1Bootstrap();
        }
        return NO_REGISTRY_BOOTSTRAP;
    }

    private static String currentMinecraftVersion() {
        try {
            // 引导阶段 Bukkit Server 尚未创建，优先使用不依赖服务器实例的构建信息。
            return ServerBuildInfo.buildInfo().minecraftVersionId();
        } catch (RuntimeException | LinkageError ignored) {
            // 保留旧分支/衍生服务端的 Bukkit 回退路径。
        }
        try {
            return Bukkit.getMinecraftVersion();
        } catch (RuntimeException | LinkageError ignored) {
            return "0.0.0";
        }
    }

}
