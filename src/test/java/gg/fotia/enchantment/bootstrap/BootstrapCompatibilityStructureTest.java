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
        assertFalse(paperPlugin.contains("PaperV1_21_R1Bootstrap"));
    }

    @Test
    void unifiedBootstrapperDoesNotDirectlyUseVersionSpecificRegistryEvent() throws IOException {
        String dispatcher = read("src/bootstrap/entrypoint/java/gg/fotia/enchantment/bootstrap/FotiaEnchantmentBootstrap.java");

        assertTrue(dispatcher.contains("Bukkit.getMinecraftVersion()"));
        assertTrue(dispatcher.contains("REGISTRY_BOOTSTRAP_MIN_VERSION"));
        assertTrue(dispatcher.contains("return NO_REGISTRY_BOOTSTRAP"));
        assertTrue(dispatcher.contains("PaperV1_21_R1Bootstrap"));
        assertTrue(dispatcher.contains("PaperV1_21_R6Bootstrap"));
        assertFalse(dispatcher.contains("ServerBuildInfo"));
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
                "src/bootstrap/shared/java/gg/fotia/enchantment/bootstrap/api/FotiaBootstrapImplementation.java")));
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "src/bootstrap/entrypoint/java/gg/fotia/enchantment/bootstrap/FotiaEnchantmentBootstrap.java")));
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "src/bootstrap/paper/v1_21_R1/java/gg/fotia/enchantment/bootstrap/paper/v1_21_R1/PaperV1_21_R1Bootstrap.java")));
        assertTrue(Files.isRegularFile(ROOT.resolve(
                "src/bootstrap/paper/v1_21_R6/java/gg/fotia/enchantment/bootstrap/paper/v1_21_R6/PaperV1_21_R6Bootstrap.java")));
        assertTrue(pom.contains("compile-versioned-bootstraps"));
        assertTrue(pom.contains("src/bootstrap/shared/java"));
        assertTrue(pom.contains("src/bootstrap/entrypoint/java"));
        assertTrue(pom.contains("src/bootstrap/paper/v1_21_R1/java"));
        assertTrue(pom.contains("src/bootstrap/paper/v1_21_R6/java"));
        assertFalse(pom.contains("<id>paper-1.21.1</id>"));
        assertFalse(pom.contains("src/compat-paper-1.21.1/java"));
        assertFalse(pom.contains("src/bootstrap/java"));
        assertFalse(pom.contains("FotiaEnchantmentBootstrapCurrent"));
        assertFalse(pom.contains("FotiaEnchantmentBootstrap1211"));
        assertFalse(pom.contains("compile-paper-1.21.1-bootstrap"));
        assertFalse(pom.contains("copy-paper-1.21.1-compat-resources"));
        assertFalse(pom.contains("${project.artifactId}-${project.version}-paper-1.21.1"));
    }

    @Test
    void versionedBootstrapsOnlyLoadBundledDefaultsWhenExternalEnchantmentsDirectoryIsMissing() throws IOException {
        assertBootstrapUsesExternalDirectoryAsAuthoritative(
                "src/bootstrap/paper/v1_21_R1/java/gg/fotia/enchantment/bootstrap/paper/v1_21_R1/PaperV1_21_R1Bootstrap.java");
        assertBootstrapUsesExternalDirectoryAsAuthoritative(
                "src/bootstrap/paper/v1_21_R6/java/gg/fotia/enchantment/bootstrap/paper/v1_21_R6/PaperV1_21_R6Bootstrap.java");
    }

    @Test
    void versionedBootstrapsEnumerateAllBundledDefaultEnchantments() throws IOException {
        assertBootstrapEnumeratesBundledDefaults(
                "src/bootstrap/paper/v1_21_R1/java/gg/fotia/enchantment/bootstrap/paper/v1_21_R1/PaperV1_21_R1Bootstrap.java");
        assertBootstrapEnumeratesBundledDefaults(
                "src/bootstrap/paper/v1_21_R6/java/gg/fotia/enchantment/bootstrap/paper/v1_21_R6/PaperV1_21_R6Bootstrap.java");
    }

    private static String read(String path) throws IOException {
        return Files.readString(ROOT.resolve(path));
    }

    private static void assertBootstrapUsesExternalDirectoryAsAuthoritative(String path) throws IOException {
        String source = read(path);
        int externalDirectory = source.indexOf("Path externalEnchantments = dataDirectory.resolve(\"enchantments\");");
        int externalCheck = source.indexOf("if (Files.isDirectory(externalEnchantments))", externalDirectory);
        int externalLoad = source.indexOf("loadExternalEnchantments(externalEnchantments, names, enchantments);", externalCheck);
        int elseBranch = source.indexOf("} else {", externalLoad);
        int bundledLoad = source.indexOf("loadBundledDefaultEnchantments(classLoader, names, enchantments);", elseBranch);
        int bootstrapDataReturn = source.indexOf("return new BootstrapData", bundledLoad);

        assertTrue(externalDirectory >= 0, path + " must resolve the external enchantments directory");
        assertTrue(externalCheck > externalDirectory, path + " must check the external directory before loading defaults");
        assertTrue(externalLoad > externalCheck, path + " must load external enchantments when the directory exists");
        assertTrue(elseBranch > externalLoad, path + " must keep bundled defaults in the missing-directory branch");
        assertTrue(bundledLoad > elseBranch, path + " must load bundled defaults only when the directory is missing");
        assertTrue(bootstrapDataReturn > bundledLoad, path + " must finish bootstrap data after choosing one source");
    }

    private static void assertBootstrapEnumeratesBundledDefaults(String path) throws IOException {
        String source = read(path);

        assertFalse(source.contains("DEFAULT_ENCHANTMENT_RESOURCES"),
                path + " must not use a stale handwritten default enchantment list");
        assertTrue(source.contains("listBundledEnchantmentResources()"),
                path + " must enumerate bundled enchantments from the jar or development output directory");
        assertTrue(source.contains("Files.walk(enchantmentDir)"),
                path + " must enumerate default enchantments from exploded development resources");
        assertTrue(source.contains("new JarFile(location.toFile())"),
                path + " must enumerate default enchantments from packaged jars");
    }
}
