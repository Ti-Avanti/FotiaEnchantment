package gg.fotia.enchantment.bootstrap;

import gg.fotia.enchantment.bootstrap.api.FotiaBootstrapImplementation;
import gg.fotia.enchantment.bootstrap.paper.v1_21_R1.PaperV1_21_R1Bootstrap;
import gg.fotia.enchantment.bootstrap.paper.v1_21_R6.PaperV1_21_R6Bootstrap;
import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FotiaEnchantmentBootstrap implements PluginBootstrap {

    private static final MinecraftVersion COMPOSE_REGISTRY_MIN_VERSION = new MinecraftVersion(1, 21, 11);

    @Override
    public void bootstrap(BootstrapContext context) {
        implementationFor(ServerBuildInfo.buildInfo().minecraftVersionId()).bootstrap(context);
    }

    static FotiaBootstrapImplementation implementationFor(String minecraftVersionId) {
        MinecraftVersion version = MinecraftVersion.parse(minecraftVersionId);
        if (version.compareTo(COMPOSE_REGISTRY_MIN_VERSION) >= 0) {
            return new PaperV1_21_R6Bootstrap();
        }
        return new PaperV1_21_R1Bootstrap();
    }

    record MinecraftVersion(int major, int minor, int patch) implements Comparable<MinecraftVersion> {

        private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?.*");

        static MinecraftVersion parse(String raw) {
            if (raw == null) {
                return new MinecraftVersion(0, 0, 0);
            }

            Matcher matcher = VERSION_PATTERN.matcher(raw.trim());
            if (!matcher.matches()) {
                return new MinecraftVersion(0, 0, 0);
            }

            int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
            return new MinecraftVersion(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    patch);
        }

        @Override
        public int compareTo(MinecraftVersion other) {
            int majorCompare = Integer.compare(major, other.major);
            if (majorCompare != 0) {
                return majorCompare;
            }
            int minorCompare = Integer.compare(minor, other.minor);
            if (minorCompare != 0) {
                return minorCompare;
            }
            return Integer.compare(patch, other.patch);
        }
    }
}
