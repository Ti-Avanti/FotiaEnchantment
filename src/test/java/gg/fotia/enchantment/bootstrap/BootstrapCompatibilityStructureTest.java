package gg.fotia.enchantment.bootstrap;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BootstrapCompatibilityStructureTest {

    private static final Path ROOT = Path.of("").toAbsolutePath();

    @Test
    void paperPluginUsesUnifiedBootstrapper() throws IOException {
        String paperPlugin = read("src/main/resources/paper-plugin.yml");

        assertTrue(paperPlugin.contains("bootstrapper: gg.fotia.enchantment.bootstrap.FotiaEnchantmentBootstrap"));
        assertFalse(paperPlugin.contains("FotiaEnchantmentBootstrap1211"));
    }

    @Test
    void unifiedBootstrapperDoesNotDirectlyUseVersionSpecificRegistryEvent() throws IOException {
        String dispatcher = read("src/bootstrap/java/gg/fotia/enchantment/bootstrap/FotiaEnchantmentBootstrap.java");

        assertTrue(dispatcher.contains("ServerBuildInfo.buildInfo()"));
        assertFalse(dispatcher.contains("RegistryEvents.ENCHANTMENT.compose()"));
        assertFalse(dispatcher.contains("RegistryEvents.ENCHANTMENT.freeze()"));
        assertFalse(dispatcher.contains("Class.forName"));
        assertFalse(dispatcher.contains("getDeclaredMethod"));
        assertFalse(dispatcher.contains("MethodHandle"));
    }

    @Test
    void bothBootstrapImplementationsArePackagedIntoSingleJarBuild() throws IOException {
        String pom = read("pom.xml");

        assertTrue(Files.isRegularFile(ROOT.resolve(
                "src/main/java/gg/fotia/enchantment/bootstrap/FotiaEnchantmentBootstrapCurrent.java")));
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "src/main/java/gg/fotia/enchantment/bootstrap/FotiaBootstrapImplementation.java")));
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "src/bootstrap/java/gg/fotia/enchantment/bootstrap/FotiaEnchantmentBootstrap.java")));
        assertTrue(pom.contains("compile-paper-1.21.1-bootstrap"));
        assertFalse(pom.contains("<id>paper-1.21.1</id>"));
        assertFalse(pom.contains("copy-paper-1.21.1-compat-resources"));
        assertFalse(pom.contains("${project.artifactId}-${project.version}-paper-1.21.1"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(ROOT.resolve(path));
    }
}
